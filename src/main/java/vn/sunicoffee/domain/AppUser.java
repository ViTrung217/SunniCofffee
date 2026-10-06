package vn.sunicoffee.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="app_users")
public class AppUser {
    @Column(nullable=false, length=20)
    private String phone = "";
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone=phone; }
    @Column(nullable=false, length=500)
    private String address = "";
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address=address; }
    @Column(nullable=false)
    private long authVersion = 0;
    public long getAuthVersion() { return authVersion; }
    public void setAuthVersion(long authVersion) { this.authVersion=authVersion; }
    @Version @Column(nullable=false)
    private Long version;
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version=version; }

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=100)
    private String fullName;
    @Column(nullable=false, unique=true, length=190)
    private String email;
    @Column(nullable=false, length=100)
    private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private Role role;
    @Column(nullable=false)
    private boolean enabled;

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
