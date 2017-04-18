package lexek.wschat.services;

import com.google.common.hash.Hashing;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.chat.model.User;
import lexek.wschat.chat.msg.EmoticonProvider;
import lexek.wschat.db.dao.EmoticonDao;
import lexek.wschat.db.model.Emoticon;
import lexek.wschat.db.tx.Transactional;
import org.jvnet.hk2.annotations.Service;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class EmoticonService implements EmoticonProvider<Emoticon> {
    private final Path emoticonsDir;
    private final Dimension maxSize;
    private final EmoticonDao emoticonDao;
    private final JournalService journalService;
    private volatile List<Emoticon> cachedEmoticons = null;

    @Inject
    public EmoticonService(
        @Named("emoticon.maxDimensions") Dimension maxSize,
        @Named("core.dataDirectory") File dataDir,
        EmoticonDao emoticonDao,
        JournalService journalService
    ) {
        this.emoticonsDir = Paths.get(dataDir.toURI()).resolve("emoticons");
        this.maxSize = maxSize;
        this.emoticonDao = emoticonDao;
        this.journalService = journalService;
    }

    @Transactional
    public boolean add(String code, File sourceFile, String originalName, User admin) throws IOException {
        Path emoticonFile = createEmoticonFile(originalName);
        Files.move(sourceFile.toPath(), emoticonFile);
        BufferedImage image = ImageIO.read(emoticonFile.toFile());
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > maxSize.getWidth() || height > maxSize.getHeight()) {
            throw new InvalidInputException("file", "Image is too big");
        } else {
            Emoticon existingEmoticon = cachedEmoticons
                .stream()
                .filter(e -> e.getCode().equals(code))
                .findAny()
                .orElse(null);
            boolean success = true;
            try {
                if (existingEmoticon == null) {
                    Emoticon emoticon = new Emoticon(null, code, emoticonFile.getFileName().toString(), height, width);
                    emoticonDao.addEmoticon(emoticon);
                    journalService.newEmoticon(admin, emoticon);
                } else {
                    emoticonDao.changeFile(
                        existingEmoticon.getId(),
                        emoticonFile.getFileName().toString(),
                        width, height
                    );
                    journalService.emoticonImageChanged(
                        admin,
                        existingEmoticon.getCode(),
                        existingEmoticon.getFileName(),
                        emoticonFile.getFileName().toString()
                    );
                }
            } catch (Exception e) {
                success = false;
            }

            if (success) {
                synchronized (this) {
                    cachedEmoticons = null;
                }
            }
            return success;
        }
    }

    @Transactional
    public void delete(long emoticonId, User admin) {
        Emoticon emoticon = emoticonDao.delete(emoticonId);
        journalService.deletedEmoticon(admin, emoticon);
        synchronized (this) {
            cachedEmoticons = null;
        }
    }

    public List<Emoticon> getEmoticons() {
        if (cachedEmoticons == null) {
            synchronized (this) {
                if (cachedEmoticons == null) {
                    cachedEmoticons = emoticonDao.getAll();
                    cachedEmoticons.forEach(Emoticon::initPattern);
                }
            }
        }
        return cachedEmoticons;
    }

    private Path createEmoticonFile(String originalName) {
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String newName = Hashing.md5()
            .newHasher()
            .putUnencodedChars(originalName)
            .putLong(System.currentTimeMillis())
            .hash() + extension;
        return emoticonsDir.resolve(newName);
    }
}
