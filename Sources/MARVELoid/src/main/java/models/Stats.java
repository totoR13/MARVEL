package models;

public class Stats {
    public int numberOfExtractedMethods;
    public int numberOfEncryptedMethods;
    public int numberOfInjectedATs;

    public Stats() {
        this.numberOfExtractedMethods = 0;
        this.numberOfEncryptedMethods = 0;
        this.numberOfInjectedATs = 0;
    }

    public void printStats() {
        System.out.println(String.format("Protection statistic: \n-) Number of extracted methods: %d;\n-) Number of encrypted methods: %d;\n-) Number of injected AT controls: %d;\n",
                this.numberOfExtractedMethods, this.numberOfEncryptedMethods, this.numberOfInjectedATs));
    }

}
