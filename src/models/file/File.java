package models.file;

import models.PeerInfo;

import java.util.ArrayList;
import java.util.List;

public final class File {
    private FileName fileName;
    private Integer size;
    private List<FileChunk> fileChunks = new ArrayList<>();

    public File(FileName fileName, int size) {
        setFileName(fileName);
        setSize(size);
        int nextCid = 1;
        while((nextCid << 20L)+1 < size){
            this.fileChunks.add(new FileChunk(fileName, nextCid++));
        }
        this.fileChunks.add(new FileChunk(fileName, nextCid));
    }




    // getters & setter

    public FileName getFileName() {
        return this.fileName;
    }

    private void setFileName(FileName fileName) {
        this.fileName = fileName;
    }


    public int getSize() {
        return this.size;
    }

    private File setSize(int size) {
        this.size = size;
        return this;
    }


    public List<FileChunk> getFileChunks() {
        return this.fileChunks;
    }

    public FileChunk getChunkByCid(int cid){
        if(!isChunkExist(cid)){
            throw new IllegalArgumentException("chunk with cid does not exist");
        }
        return fileChunks.get(cid);
    }

    public boolean isChunkExist(int cid){
        return 0 <= cid && cid < fileChunks.size();
    }
}
