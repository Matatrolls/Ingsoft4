package co.edu.icesi.mio.infra.csv;

public class Arco {
    private Parada paradaOrigen;
    private Parada paradaDestino;
    private int lineId;
    private String lineShortName;
    private int orientation;
    private int sequenceOrigen;
    private int sequenceDestino;

    public Arco(Parada var1, Parada var2, int var3, String var4, int var5, int var6, int var7) {
        this.paradaOrigen = var1;
        this.paradaDestino = var2;
        this.lineId = var3;
        this.lineShortName = var4;
        this.orientation = var5;
        this.sequenceOrigen = var6;
        this.sequenceDestino = var7;
    }

    public Parada getParadaOrigen() {
        return this.paradaOrigen;
    }

    public Parada getParadaDestino() {
        return this.paradaDestino;
    }

    public int getLineId() {
        return this.lineId;
    }

    public String getLineShortName() {
        return this.lineShortName;
    }

    public int getOrientation() {
        return this.orientation;
    }

    public int getSequenceOrigen() {
        return this.sequenceOrigen;
    }

    public int getSequenceDestino() {
        return this.sequenceDestino;
    }

    public String getOrientationName() {
        return this.orientation == 0 ? "IDA" : "VUELTA";
    }

    public String toString() {
        return String.format("Arco [Ruta: %s (%s), %s -> %s, Seq: %d -> %d, StopIds: %d -> %d]", this.lineShortName, this.getOrientationName(), this.paradaOrigen.getShortName(), this.paradaDestino.getShortName(), this.sequenceOrigen, this.sequenceDestino, this.paradaOrigen.getStopId(), this.paradaDestino.getStopId());
    }

    public boolean equals(Object var1) {
        if (this == var1) {
            return true;
        } else if (var1 != null && this.getClass() == var1.getClass()) {
            Arco var2 = (Arco)var1;
            return this.lineId == var2.lineId && this.orientation == var2.orientation && this.paradaOrigen.equals(var2.paradaOrigen) && this.paradaDestino.equals(var2.paradaDestino);
        } else {
            return false;
        }
    }

    public int hashCode() {
        int var1 = this.paradaOrigen.hashCode();
        var1 = 31 * var1 + this.paradaDestino.hashCode();
        var1 = 31 * var1 + this.lineId;
        var1 = 31 * var1 + this.orientation;
        return var1;
    }
}
