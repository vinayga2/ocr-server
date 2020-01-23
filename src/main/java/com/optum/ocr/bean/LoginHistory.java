package com.optum.ocr.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "LoginHistory")
public class LoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "LoginHistoryId")
    public Long LoginHistoryId;

    public String msId;
    public LocalDate lastLogin;
}
