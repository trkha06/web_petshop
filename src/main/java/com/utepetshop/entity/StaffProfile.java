// File: src/main/java/com/utepetshop/entity/StaffProfile.java
package com.utepetshop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staff_profiles")
@Getter
@Setter
@NoArgsConstructor
public class StaffProfile {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "branch_id", nullable = false)
  private Branch branch;

  public StaffProfile(User user, Branch branch) {
    this.user = user;
    this.branch = branch;
  }
}
