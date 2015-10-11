package lexek.wschat.services;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import lexek.wschat.db.dao.EmoticonDao;
import lexek.wschat.db.jooq.tables.pojos.Emoticon;
import lexek.wschat.db.model.UserDto;
import lexek.wschat.db.model.e.InvalidInputException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EmoticonService {
    private final Path emoticonsDir;
    private final Dimension maxSize;
    private final EmoticonDao emoticonDao;
    private final JournalService journalService;

    public EmoticonService(Dimension maxSize, File dataDir, EmoticonDao emoticonDao, JournalService journalService) {
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
            throw new InvalidInputException(ImmutableMap.of("file", "Image is too big"));
        } else {
            boolean success = true;
            try {
                Emoticon emoticon = new Emoticon(null, code, emoticonFile.getFileName().toString(), height, width);
                emoticonDao.addEmoticon(emoticon);
                journalService.newEmoticon(admin, emoticon);
            } catch (Exception e) {
                success = false;
            }
            return success;
        }
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

    public void delete(long emoticonId, UserDto admin) {
        Emoticon emoticon = emoticonDao.delete(emoticonId);
        journalService.deletedEmoticon(admin, emoticon);
    }

    public String getAllAsJson() {
        return emoticonDao.getAllAsJson();
    }
}
