package models.file;

public final class FileName {
    private String fileName;

    public FileName(String fileName) {
        setFileName(fileName);
    }


    // getters & setter

    public String getFileName() {
        return this.fileName;
    }

    private FileName setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }
}
