package am.loras.backend.dto.response;

public class AdminUserResponse {

    private Long id;
    private String username;
    private boolean mustChangePassword;

    public AdminUserResponse() {
    }

    public AdminUserResponse(Long id, String username, boolean mustChangePassword) {
        this.id = id;
        this.username = username;
        this.mustChangePassword = mustChangePassword;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
