public enum MushroomClass {
    E("edible"),
    P("poisonous");

    private final String description;

    MushroomClass(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
    public static MushroomClass fromCode(String code) {
        return switch (code.toLowerCase()) {
            case "e" -> E;
            case "p" -> P;
            default -> throw new IllegalArgumentException("Unknown code: " + code);
        };
    }
}
