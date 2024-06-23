package models.file;

public final class FileChunk {
    private FileName fileName;
    private Integer cid;

    public FileChunk(FileName fileName, int cid) {
        setFileName(fileName);
        setCid(cid);
    }


    // getters & setter

    public FileName getFileName() {
        return this.fileName;
    }

    private FileChunk setFileName(FileName fileName) {
        this.fileName = fileName;
        return this;
    }


    public int getCid() {
        return this.cid;
    }

    private FileChunk setCid(int cid) {
        this.cid = cid;
        return this;
    }
}
