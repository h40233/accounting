package com.lemoooooon.accounting.dto;

import com.lemoooooon.accounting.model.Family;
import com.lemoooooon.accounting.model.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyDetailDto {
    private Long id;
    private String name;
    private String inviteCode;
    private MemberDto host; // 使用簡化的 MemberDto 避免洩漏過多資訊

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDto {
        private String nickname;
        private String avatarUrl;
    }

    public static FamilyDetailDto fromEntity(Family family) {
        Member hostEntity = family.getHost();
        MemberDto hostDto = MemberDto.builder()
                .nickname(hostEntity.getNickname())
                .avatarUrl(hostEntity.getAvatarUrl())
                .build();

        return FamilyDetailDto.builder()
                .id(family.getId())
                .name(family.getName())
                .inviteCode(family.getInviteCode())
                .host(hostDto)
                .build();
    }
}
