package demo;

public class UserDTO {
    private String name;

    public UserDTO() {
        // Quarkus/Jackson/JSON-B need a no-arg constructor
    }

    public UserDTO(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
