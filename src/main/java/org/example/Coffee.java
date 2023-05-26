package org.example;

public class Coffee {

    private int id;
    private String country;
    private String region;
    private String type;
    private double aroma;
    private double acidity;
    private double sweetness;
    private double score;
    private double priceKg;
    private String producer;
    private int mass;
    private int inStorage;
    private int amount;

    public Coffee(int id, String country, String region, String type, double aroma, double acidity, double sweetness, double score, double priceKg, String producer, int mass, int inStorage, int amount) {
        this.id = id;
        this.country = country;
        this.region = region;
        this.type = type;
        this.aroma = aroma;
        this.acidity = acidity;
        this.sweetness = sweetness;
        this.score = score;
        this.priceKg = priceKg;
        this.producer = producer;
        this.mass = mass;
        this.inStorage = inStorage;
        this.amount = amount;
    }

    public Coffee() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAroma() {
        return aroma;
    }

    public void setAroma(double aroma) {
        this.aroma = aroma;
    }

    public double getAcidity() {
        return acidity;
    }

    public void setAcidity(double acidity) {
        this.acidity = acidity;
    }

    public double getSweetness() {
        return sweetness;
    }

    public void setSweetness(double sweetness) {
        this.sweetness = sweetness;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getPriceKg() {
        return priceKg;
    }

    public void setPriceKg(double priceKg) {
        this.priceKg = priceKg;
    }

    public int getMass() {
        return mass;
    }

    public void setMass(int mass) {
        this.mass = mass;
    }

    public int getInStorage() {
        return inStorage;
    }

    public void setInStorage(int inStorage) {
        this.inStorage = inStorage;
    }

    public String getProducer() {
        return producer;
    }

    public void setProducer(String producer) {
        this.producer = producer;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Id: " + id +
                ", Kraj: " + country +
                ", Region: " + region +
                ", Typ: " + type +
                ", Aromat: " + aroma +
                ", Kwasowość: " + acidity +
                ", Słodycz: " + sweetness +
                ", Ocena: " + score +
                ", Cena za 1kg: " + priceKg +
                ", Masa paczki: " + mass +
                ", W koszyku: " + amount;
    }
}
