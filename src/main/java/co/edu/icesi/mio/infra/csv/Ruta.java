package co.edu.icesi.mio.infra.csv;


public class Ruta {
    private int lineId;
    private int planVersionId;
    private String shortName;
    private String description;

    public Ruta(int var1, int var2, String var3, String var4) {
        this.lineId = var1;
        this.planVersionId = var2;
        this.shortName = var3;
        this.description = var4;
    }

    public int getLineId() {
        return this.lineId;
    }

    public int getPlanVersionId() {
        return this.planVersionId;
    }

    public String getShortName() {
        return this.shortName;
    }

    public String getDescription() {
        return this.description;
    }

    public String toString() {
        return "Ruta{lineId=" + this.lineId + ", shortName='" + this.shortName + "', description='" + this.description + "'}";
    }

    public boolean equals(Object var1) {
        if (this == var1) {
            return true;
        } else if (var1 != null && this.getClass() == var1.getClass()) {
            Ruta var2 = (Ruta)var1;
            return this.lineId == var2.lineId;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Integer.hashCode(this.lineId);
    }
}
