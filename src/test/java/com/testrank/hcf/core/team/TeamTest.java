package com.testrank.hcf.core.team;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TeamTest {
    @Test
    void membersAndRolesMutatePredictably() {
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Team team = new Team(UUID.randomUUID(), "TestTeam", leader);

        team.member(member, TeamRole.CAPTAIN);

        assertTrue(team.isMember(leader));
        assertTrue(team.isMember(member));
        assertEquals(TeamRole.LEADER, team.members().get(leader));
        assertEquals(TeamRole.CAPTAIN, team.members().get(member));

        team.removeMember(member);
        assertFalse(team.isMember(member));
    }
}
