package com.saaspe.Adaptor.Entity;

import javax.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "saaspe_atlassian_users")


@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class AtlassianUsers {
    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "user_email")
    private String userEmail;    
    
}