package co.edu.icesi.mio.concurrency.datagram;

/**
 * Representa un chunk (pedazo) de un archivo para procesamiento paralelo.
 */
public class FileChunk {
    private final String filePath;
    private final long startLine;
    private final long endLine;
    private final int chunkId;

    public FileChunk(String filePath, long startLine, long endLine, int chunkId) {
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.chunkId = chunkId;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getStartLine() {
        return startLine;
    }

    public long getEndLine() {
        return endLine;
    }

    public int getChunkId() {
        return chunkId;
    }

    public long getLineCount() {
        return endLine - startLine;
    }

    @Override
    public String toString() {
        return String.format("Chunk[%d, lines %d-%d (%d lines)]",
                chunkId, startLine, endLine, getLineCount());
    }
}
