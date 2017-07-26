package com.ecm.alfresco.migration.bean.counter;

public class Counter {
    private int counterProcessed = 0;
    private int counterMigrated = 0;
    private int counterSkipped = 0;
    private int counterReplaced = 0;
    private int counterNew = 0;
    private int counterFailed = 0;
    private int counterExtractedDocs = 0;
    private int counterExtractedFolders = 0;
    private int pageIndex = 0;

    public synchronized int increasePageIndex() {
        return this.pageIndex++;
    }

    public synchronized int increaseCounterExtractedDocs() {
        return this.counterExtractedDocs++;
    }

    public synchronized int increaseCounterExtractedFolders() {
        return this.counterExtractedFolders++;
    }

    public synchronized int increaseCounterProcessed() {
        return this.counterProcessed++;
    }

    public synchronized int increaseCounterMigrated() {
        return this.counterMigrated++;
    }

    public synchronized int increaseCounterSkipped() {
        return this.counterSkipped++;
    }

    public synchronized int increaseCounterReplaced() {
        return this.counterReplaced++;
    }

    public synchronized int increaseCounterNew() {
        return this.counterNew++;
    }

    public synchronized int increaseCounterFailed() {
        return this.counterFailed++;
    }

    public int getCounterProcessed() {
        return counterProcessed;
    }

    public int getCounterMigrated() {
        return counterMigrated;
    }

    public int getCounterSkipped() {
        return counterSkipped;
    }
    public int getCounterReplaced() {
        return counterReplaced;
    }


    public int getCounterNew() {
        return counterNew;
    }

    public int getCounterFailed() {
        return counterFailed;
    }

    public int getCounterExtractedDocs() {
        return counterExtractedDocs;
    }

    public int getCounterExtractedFolders() {
        return counterExtractedFolders;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void resetPageIndex() {
        pageIndex = 0;
    }
}
