package co.edu.icesi.mio.infra.csv;

public class Parada {
    private int stopId;
    private int planVersionId;
    private String shortName;
    private String longName;
    private double decimalLongitude;
    private double decimalLatitude;

    public Parada(int var1, int var2, String var3, String var4, double var5, double var7) {
        this.stopId = var1;
        this.planVersionId = var2;
        this.shortName = var3;
        this.longName = var4;
        this.decimalLongitude = var5;
        this.decimalLatitude = var7;
    }

    public int getStopId() {
        return this.stopId;
    }

    public int getPlanVersionId() {
        return this.planVersionId;
    }

    public String getShortName() {
        return this.shortName;
    }

    public String getLongName() {
        return this.longName;
    }

    public double getDecimalLongitude() {
        return this.decimalLongitude;
    }

    public double getDecimalLatitude() {
        return this.decimalLatitude;
    }

    public String toString() {
        return "Parada{stopId=" + this.stopId + ", shortName='" + this.shortName + "', longName='" + this.longName + "'}";
    }

    public boolean equals(Object var1) {
        if (this == var1) {
            return true;
        } else if (var1 != null && this.getClass() == var1.getClass()) {
            Parada var2 = (Parada)var1;
            return this.stopId == var2.stopId;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Integer.hashCode(this.stopId);
    }
}
