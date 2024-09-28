package dtos;

public class UserDTO {
    private String user_id;
    private String user_name;

    public UserDTO(String user_id, String user_name) {
        this.user_id = user_id;
        this.user_name = user_name;
    }

    public String getId() {
        return user_id;
    }

    public String setId(String user_id) {
        return this.user_id = user_id;
    }

    public String getUsername() {
        return user_name;
    }

    public String setUsername(String user_name) {
        return this.user_name = user_name;
    }

}
