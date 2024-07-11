package com.saaspe.Adaptor.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GitHubInviteRequestBody {
	   private String role;
	    private String email;
	    private int[] teamIds;
	    private int inviteeId;
}
