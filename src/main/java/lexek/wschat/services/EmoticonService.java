package lexek.wschat.services;

import com.google.common.hash.Hashing;
import lexek.wschat.chat.Message;
import lexek.wschat.chat.MessageBroadcaster;
import lexek.wschat.chat.e.InvalidInputException;
import lexek.wschat.db.dao.EmoticonDao;
import lexek.wschat.db.model.Emoticon;
import lexek.wschat.db.model.UserDto;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class EmoticonService {
    private final Path emoticonsDir;
    private final Dimension maxSize;
    private final EmoticonDao emoticonDao;
    private final JournalService journalService;
    private final MessageBroadcaster messageBroadcaster;
    private volatile List<Emoticon> cachedEmoticons = null;

    public EmoticonService(
        Dimension maxSize,
        File dataDir,
        EmoticonDao emoticonDao,
        JournalService journalService,
        MessageBroadcaster messageBroadcaster
    ) {
        this.messageBroadcaster = messageBroadcaster;
        this.emoticonsDir = Paths.get(dataDir.toURI()).resolve("emoticons");
        this.maxSize = maxSize;
        this.emoticonDao = emoticonDao;
        this.journalService = journalService;
    }

    public boolean add(String code, File sourceFile, String originalName, UserDto admin) throws IOException {
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
                }
            } catch (Exception e) {
                success = false;
            }

            if (success) {
                synchronized (this) {
                    cachedEmoticons = null;
                }
                sendEmoticons();
            }
            return success;
        }
    }

    public void delete(long emoticonId, UserDto admin) {
        Emoticon emoticon = emoticonDao.delete(emoticonId);
        journalService.deletedEmoticon(admin, emoticon);
        synchronized (this) {
            cachedEmoticons = null;
        }
        sendEmoticons();
    }

    public List<Emoticon> getEmoticons() {
        if (cachedEmoticons == null) {
            synchronized (this) {
                if (cachedEmoticons == null) {
                    cachedEmoticons = emoticonDao.getAll();
                }
            }
        }
        return cachedEmoticons;
    }

    private Path createEmoticonFile(String originalName) throws IOException {
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String newName = Hashing.md5()
            .newHasher()
            .putUnencodedChars(originalName)
            .putLong(System.currentTimeMillis())
            .hash() + extension;
        return emoticonsDir.resolve(newName);
    }

    private void sendEmoticons() {
        messageBroadcaster.submitMessage(Message.emoticonsMessage(getEmoticons()));
    }
}
