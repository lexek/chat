package lexek.wschat.services;

import lexek.wschat.db.JournalDao;
import lexek.wschat.db.jooq.tables.pojos.Journal;

public class JournalService {
    private final JournalDao journalDao;

    public JournalService(JournalDao journalDao) {
        this.journalDao = journalDao;
    }

    public void journal(Journal journal) {
        journalDao.add(journal);
    }
}
