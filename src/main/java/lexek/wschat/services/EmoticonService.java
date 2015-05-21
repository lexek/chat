package lexek.wschat.services;

import lexek.wschat.db.dao.EmoticonDao;
import lexek.wschat.db.jooq.tables.pojos.Emoticon;
import lexek.wschat.db.model.UserDto;

public class EmoticonService {
    private final EmoticonDao emoticonDao;
    private final JournalService journalService;

    public EmoticonService(EmoticonDao emoticonDao, JournalService journalService) {
        this.emoticonDao = emoticonDao;
        this.journalService = journalService;
    }

    public void add(Emoticon emoticon, UserDto admin) {
        emoticonDao.addEmoticon(emoticon);
        journalService.newEmoticon(admin, emoticon);
    }

    public void delete(long emoticonId, UserDto admin) {
        Emoticon emoticon = emoticonDao.delete(emoticonId);
        journalService.deletedEmoticon(admin, emoticon);
    }

    public String getAllAsJson() {
        return emoticonDao.getAllAsJson();
    }
}
