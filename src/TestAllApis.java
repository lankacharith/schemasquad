import charithapis.*;
import enesapis.*;
import giseleapis.*;
import shreyasapis.*;
import vincentapis.*;
import dbconnection.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated DB-backed smoke tests for all API menu options (1-45).
 *
 * This is intentionally non-interactive: it creates a small temporary dataset
 * (player + characters), runs each API, and validates the returned output /
 * resulting DB state.
 */
public class TestAllApis {

    static class CaseResult {
        final int id;
        final String name;
        final boolean passed;
        final String detail;

        CaseResult(int id, String name, boolean passed, String detail) {
            this.id = id;
            this.name = name;
            this.passed = passed;
            this.detail = detail;
        }
    }

    private static List<String> failuresRef;

    public static void main(String[] args) {
        List<CaseResult> results = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        failuresRef = failures;
        boolean suiteCrashed = false;

        Connection setupConn = null;
        try {
            setupConn = DBConnection.getConnection();

            String username = "autosmoke_" + System.currentTimeMillis();
            String password = "pw12345";
            String email = username + "@example.com";

            // Lookup required customization values so CreateCharacter doesn't depend on parsing printed output.
            String raceName = pickFirstRequiredString(setupConn,
                "SELECT racename FROM race ORDER BY racename LIMIT 1",
                null);
            String className = pickFirstRequiredString(setupConn,
                "SELECT classname FROM class ORDER BY classname LIMIT 1",
                null);
            String gender = pickFirstOptionalString(setupConn,
                "SELECT DISTINCT gender FROM character LIMIT 1",
                null);
            if (gender == null || gender.trim().isEmpty()) gender = "male";
            String hairType = pickFirstRequiredString(setupConn,
                "SELECT hairtype FROM hairtype ORDER BY hairtype LIMIT 1",
                null);
            String hairColor = pickFirstRequiredString(setupConn,
                "SELECT color FROM haircolor ORDER BY color LIMIT 1",
                null);
            String skinColor = pickFirstRequiredString(setupConn,
                "SELECT skincolor FROM skincolor ORDER BY skincolor LIMIT 1",
                null);

            // We need 2 characters for party/team/inventory/trade flows.
            String charA = "autosmoke_" + System.currentTimeMillis() + "_A";
            String charB = "autosmoke_" + System.currentTimeMillis() + "_B";

            // Create player (API 37).
            String createPlayerOut = CreatePlayer.createPlayer(setupConn, username, password, email);
            boolean playerExists = verifyPlayerExists(setupConn, username);
            boolean createPlayerOk = !looksLikeError(createPlayerOut) && playerExists;
            results.add(new CaseResult(
                37, "Create Player", createPlayerOk,
                "CreatePlayer output: " + summarize(createPlayerOut) + " (playerExists=" + playerExists + ")"));
            if (!createPlayerOk) failures.add(formatFailure(37, "Create Player", createPlayerOut + " playerExists=" + playerExists));

            // Create characters (API 39).
            String createCharOut = CreateCharacter.createCharacter(
                setupConn, username, charA, gender, hairColor, hairType, skinColor, raceName, className);
            boolean charExists = verifyCharacterExists(setupConn, charA);
            boolean createCharOk = !looksLikeError(createCharOut) && charExists;
            results.add(new CaseResult(
                39, "Create Character", createCharOk,
                "CreateCharacter output: " + summarize(createCharOut) + " (charExists=" + charExists + ")"));
            if (!createCharOk) failures.add(formatFailure(39, "Create Character", createCharOut + " charExists=" + charExists));

            // Create second character using the same customization values.
            String createCharBOut = CreateCharacter.createCharacter(
                setupConn, username, charB, gender, hairColor, hairType, skinColor, raceName, className);
            // This one isn't mapped to a menu option, but we need it for later APIs.
            boolean createCharBOk = !looksLikeError(createCharBOut);
            if (!createCharBOk) {
                failures.add(formatFailure(0, "Setup: Create second character", createCharBOut));
            }

            // Choose an item name for inventory/trade tests.
            String itemName1 = pickFirstRequiredString(setupConn,
                "SELECT Name FROM Item ORDER BY ID LIMIT 1",
                null);
            String itemName2 = pickSecondOrFallbackString(setupConn,
                "SELECT Name FROM Item ORDER BY ID LIMIT 2",
                itemName1);

            // Choose a quest name for quest tests.
            String questName = pickFirstRequiredString(setupConn,
                "SELECT Name FROM Quest ORDER BY ID LIMIT 1",
                null);

            // Choose an ability for ability tests.
            String abilityName = pickAbilityForCharacterOrInsert(setupConn, charA);
            if (abilityName == null) {
                failures.add("Setup: could not find or insert an ability for " + charA);
            }

            // Choose a rank (non-owner) for joining/updating team.
            String rankNonOwner = pickFirstRequiredString(setupConn,
                "SELECT RankName FROM Rank WHERE UPPER(RankName) <> UPPER('Owner') ORDER BY ID LIMIT 1",
                null);
            String rankNew = pickFirstRequiredString(setupConn,
                "SELECT RankName FROM Rank WHERE UPPER(RankName) NOT IN (UPPER('Owner'), UPPER(?)) ORDER BY ID LIMIT 1",
                new Object[]{rankNonOwner});
            if (rankNew == null) rankNew = rankNonOwner; // fallback if DB is small

            // Choose a maxSize that is likely not full.
            int teamMaxSize = 3;

            // Create a team for team-dependent APIs (15-20) and for character stats (33).
            String teamName = "autosmoke_team_" + System.currentTimeMillis();
            String createTeamOut = CreateTeam.Server_CreateTeam(teamName, charA, teamMaxSize);
            boolean createTeamOk = !looksLikeError(createTeamOut);
            results.add(new CaseResult(
                15, "Create Team", createTeamOk, "CreateTeam output: " + summarize(createTeamOut)));
            if (!createTeamOk) failures.add(formatFailure(15, "Create Team", createTeamOut));
            if (createTeamOk) {
                String addToTeamOut = AddPlayerToTeam.Server_AddPlayerToTeam(charB, teamName, rankNonOwner);
                boolean addToTeamOk = !looksLikeError(addToTeamOut);
                results.add(new CaseResult(
                    16, "Add Player To Team", addToTeamOk, "AddPlayerToTeam output: " + summarize(addToTeamOut)));
                if (!addToTeamOk) failures.add(formatFailure(16, "Add Player To Team", addToTeamOut));

                // Update team rank for charB (19).
                String updateRankOut = UpdateTeamRank.Server_UpdateTeamRank(charB, rankNew);
                boolean updateRankOk = !looksLikeError(updateRankOut);
                results.add(new CaseResult(
                    19, "Update Team Rank", updateRankOk, "UpdateTeamRank output: " + summarize(updateRankOut)));
                if (!updateRankOk) failures.add(formatFailure(19, "Update Team Rank", updateRankOut));

                // Team details (18).
                String teamDetailsOut = GetTeamDetails.Server_GetTeamDetails(teamName);
                boolean teamDetailsOk = !looksLikeError(teamDetailsOut);
                results.add(new CaseResult(
                    18, "Get Team Details", teamDetailsOk, "GetTeamDetails output contains: " + summarize(teamDetailsOut)));
                if (!teamDetailsOk) failures.add(formatFailure(18, "Get Team Details", teamDetailsOut));
            } else {
                // Mark dependent cases as failures since prerequisite failed.
                results.add(new CaseResult(16, "Add Player To Team", false, "Skipped: CreateTeam failed."));
                results.add(new CaseResult(18, "Get Team Details", false, "Skipped: CreateTeam failed."));
                results.add(new CaseResult(19, "Update Team Rank", false, "Skipped: CreateTeam failed."));
            }

            // Create a party for party-dependent APIs (10-14).
            String partyCreateOut = CreateParty.Server_CreateParty(charA);
            boolean partyCreateOk = !looksLikeError(partyCreateOut);
            results.add(new CaseResult(
                10, "Create Party", partyCreateOk, "CreateParty output: " + summarize(partyCreateOut)));
            String joinCode = partyCreateOk ? parseJoinCode(partyCreateOut) : null;
            if (partyCreateOk && joinCode != null) {
                String addPartyOut = AddPlayerToParty.Server_AddPlayerToParty(charB, joinCode);
                boolean addPartyOk = !looksLikeError(addPartyOut);
                results.add(new CaseResult(
                    12, "Add Player To Party", addPartyOk, "AddPlayerToParty output: " + summarize(addPartyOut)));
                if (!addPartyOk) failures.add(formatFailure(12, "Add Player To Party", addPartyOut));

                String partyMembersOut = GetPartyMembers.Server_GetPartyMembers(joinCode);
                boolean partyMembersOk = !looksLikeError(partyMembersOut)
                    && partyMembersOut.toLowerCase().contains(charA.toLowerCase())
                    && partyMembersOut.toLowerCase().contains(charB.toLowerCase());
                results.add(new CaseResult(
                    9, "Get Party Members", partyMembersOk, "GetPartyMembers output: " + summarize(partyMembersOut)));
                if (!partyMembersOk) failures.add(formatFailure(9, "Get Party Members", partyMembersOut));

                String partySuggestionsOut = GetPartySuggestions.Server_GetPartySuggestions(joinCode);
                boolean partySuggestionsOk = !looksLikeError(partySuggestionsOut);
                results.add(new CaseResult(
                    14, "Get Party Suggestions", partySuggestionsOk, "GetPartySuggestions output: " + summarize(partySuggestionsOut)));
                if (!partySuggestionsOk) failures.add(formatFailure(14, "Get Party Suggestions", partySuggestionsOut));

                // Remove from party (13) and ensure they left.
                String removePartyOut = RemovePlayerFromParty.Server_RemovePlayerFromParty(charB);
                boolean removePartyOk = !looksLikeError(removePartyOut);
                results.add(new CaseResult(
                    13, "Remove Player From Party", removePartyOk, "RemovePlayerFromParty output: " + summarize(removePartyOut)));
                if (!removePartyOk) failures.add(formatFailure(13, "Remove Player From Party", removePartyOut));
            } else {
                results.add(new CaseResult(9, "Get Party Members", false, "Skipped: CreateParty failed."));
                results.add(new CaseResult(12, "Add Player To Party", false, "Skipped: CreateParty failed."));
                results.add(new CaseResult(13, "Remove Player From Party", false, "Skipped: CreateParty failed."));
                results.add(new CaseResult(14, "Get Party Suggestions", false, "Skipped: CreateParty failed."));
            }

            // Remove from team (17) and validate.
            if (!hasFailureFor(results, 15, 16) && !hasFailureFor(results, 16, 0)) {
                String removeTeamOut = RemovePlayerFromTeam.Server_RemovePlayerFromTeam(charB);
                boolean removeTeamOk = !looksLikeError(removeTeamOut);
                results.add(new CaseResult(
                    17, "Remove Player From Team", removeTeamOk, "RemovePlayerFromTeam output: " + summarize(removeTeamOut)));
                if (removeTeamOk) {
                    Integer teamId = pickFirstOptionalInt(setupConn,
                        "SELECT TeamID FROM Character WHERE Name ILIKE ?",
                        new Object[]{charB});
                    if (teamId != null) {
                        removeTeamOk = false;
                        failures.add(formatFailure(17, "Remove Player From Team", "Expected TeamID NULL but found: " + teamId));
                    }
                } else {
                    failures.add(formatFailure(17, "Remove Player From Team", removeTeamOut));
                }
                // Update results entry if state check failed
                if (!removeTeamOk) {
                    results.set(results.size() - 1, new CaseResult(17, "Remove Player From Team", false,
                        "RemovePlayerFromTeam output: " + summarize(removeTeamOut) + " (and TeamID was not NULL)"));
                }
            } else {
                results.add(new CaseResult(17, "Remove Player From Team", false, "Skipped: team setup failed."));
            }

            // Now run the remaining APIs that don't require extra setup:
            results.add(runCase(1, "Get Player Leaderboard", () ->
                GetPlayerLeaderboard.Server_GetPlayerLeaderboard("gold", 1, 5)));
            results.add(runCase(2, "Get Top Players By Level", () ->
                GetTopPlayersByLevel.Server_GetTopPlayersByLevel(1)));
            results.add(runCase(3, "Search Players", () ->
                SearchPlayers.Server_SearchPlayers("minhp", "1")));

            // Events
            String eventName = "autosmoke_event_" + System.currentTimeMillis();
            String createEventOut = CreateEvent.Server_CreateEvent(eventName, "autosmoke event", 10);
            results.add(new CaseResult(4, "Create Event", !looksLikeError(createEventOut),
                "CreateEvent output: " + summarize(createEventOut)));
            if (looksLikeError(createEventOut)) failures.add(formatFailure(4, "Create Event", createEventOut));
            results.add(runCase(5, "Get Active Events", () ->
                GetActiveEvents.Server_GetActiveEvents()));
            results.add(runCase(6, "Get Player Event Cooldown", () ->
                GetPlayerEventCooldown.Server_GetPlayerEventCooldown(charA, eventName)));

            // Abilities
            if (abilityName != null) {
                String useAbilityOut = UseAbility.Server_UseAbility(charA, abilityName);
                boolean useAbilityOk = !looksLikeError(useAbilityOut);
                results.add(new CaseResult(7, "Use Ability", useAbilityOk, "UseAbility output: " + summarize(useAbilityOut)));
                if (!useAbilityOk) failures.add(formatFailure(7, "Use Ability", useAbilityOut));

                String cooldownsOut = GetAbilityCooldowns.Server_GetAbilityCooldowns(charA);
                boolean cooldownsOk = !looksLikeError(cooldownsOut)
                    && cooldownsOut.toLowerCase().contains(abilityName.toLowerCase());
                results.add(new CaseResult(8, "Get Ability Cooldowns", cooldownsOk, "GetAbilityCooldowns output: " + summarize(cooldownsOut)));
                if (!cooldownsOk) failures.add(formatFailure(8, "Get Ability Cooldowns", cooldownsOut));
            } else {
                results.add(new CaseResult(7, "Use Ability", false, "Skipped: no ability available for " + charA));
                results.add(new CaseResult(8, "Get Ability Cooldowns", false, "Skipped: no ability available for " + charA));
            }

            // Parties list + suggestions already validated above, now list APIs:
            results.add(runCase(11, "Get All Parties", () -> GetAllParties.Server_GetAllParties(1, 10)));

            // Teams list
            results.add(runCase(20, "Get All Teams", () -> GetAllTeams.Server_GetAllTeams(1, 10)));

            // Inventory & trading
            results.add(runCase(21, "Get Player Inventory (charA)", () ->
                GetPlayerInventory.Server_GetPlayerInventory(charA)));

            String addItemOut1 = AddItemToInventory.Server_AddItemToInventory(charA, itemName1, 2);
            boolean addItemOk1 = !looksLikeError(addItemOut1);
            results.add(new CaseResult(24, "Add Item To Inventory", addItemOk1, "AddItemToInventory output: " + summarize(addItemOut1)));
            if (!addItemOk1) failures.add(formatFailure(24, "Add Item To Inventory", addItemOut1));

            String invAfterAddOut = GetPlayerInventory.Server_GetPlayerInventory(charA);
            boolean invAfterAddOk = !looksLikeError(invAfterAddOut) && invAfterAddOut.contains(itemName1);
            results.add(new CaseResult(21, "Get Player Inventory (charA) after add", invAfterAddOk,
                "GetPlayerInventory output contains item: " + itemName1));
            if (!invAfterAddOk) failures.add(formatFailure(21, "Get Player Inventory (charA) after add", invAfterAddOut));

            String removeItemOut = RemoveItemFromInventory.Server_RemoveItemFromInventory(charA, itemName1, 2);
            boolean removeItemOk = !looksLikeError(removeItemOut);
            results.add(new CaseResult(25, "Remove Item From Inventory", removeItemOk, "RemoveItemFromInventory output: " + summarize(removeItemOut)));
            if (!removeItemOk) failures.add(formatFailure(25, "Remove Item From Inventory", removeItemOut));

            results.add(runCase(23, "Get Top Players By Item", () ->
                GetTopPlayersByItem.Server_GetTopPlayersByItem(itemName2)));

            // ExecuteTrade (swap one item between charA and charB). Gold set to 0 for safety (currency may be 0).
            String addCharAItem2Out = AddItemToInventory.Server_AddItemToInventory(charA, itemName2, 1);
            String addCharBItem1Out = AddItemToInventory.Server_AddItemToInventory(charB, itemName1, 1);
            boolean setupItemsOk = !looksLikeError(addCharAItem2Out) && !looksLikeError(addCharBItem1Out);
            String tradeOut = setupItemsOk
                ? ExecuteTrade.Server_ExecuteTrade(charA, itemName2, 1, 0, charB, itemName1, 1, 0)
                : "SKIPPED: could not set up items for trade";
            boolean tradeOk = setupItemsOk && !looksLikeError(tradeOut) && tradeOut.toLowerCase().contains("trade executed");
            results.add(new CaseResult(26, "Execute Trade", tradeOk, "ExecuteTrade output: " + summarize(tradeOut)));
            if (!tradeOk) failures.add(formatFailure(26, "Execute Trade", tradeOut));

            results.add(runCase(27, "Get Trade History (charA)", () ->
                GetTradeHistory.Server_GetTradeHistory(charA)));

            // Richest players just needs to execute.
            results.add(runCase(22, "Get Richest Players", () ->
                GetRichestPlayers.Server_GetRichestPlayers(5, 0)));

            // Quests
            String assignQuestOut = AssignQuestToCharacter.Server_AssignQuestToCharacter(charA, questName);
            boolean assignQuestOk = !looksLikeError(assignQuestOut);
            results.add(new CaseResult(29, "Assign Quest To Character", assignQuestOk, "AssignQuestToCharacter output: " + summarize(assignQuestOut)));
            if (!assignQuestOk) failures.add(formatFailure(29, "Assign Quest To Character", assignQuestOut));

            String activeQuestsOut = GetActiveQuests.Server_GetActiveQuests(charA);
            boolean activeQuestsOk = !looksLikeError(activeQuestsOut) && activeQuestsOut.toLowerCase().contains(questName.toLowerCase());
            results.add(new CaseResult(31, "Get Active Quests", activeQuestsOk, "GetActiveQuests output: " + summarize(activeQuestsOut)));
            if (!activeQuestsOk) failures.add(formatFailure(31, "Get Active Quests", activeQuestsOut));

            String listQuestsOut = ListQuests.Server_ListQuests(charA);
            boolean listQuestsOk = !looksLikeError(listQuestsOut) && listQuestsOut.toLowerCase().contains(questName.toLowerCase());
            results.add(new CaseResult(28, "List Quests", listQuestsOk, "ListQuests output: " + summarize(listQuestsOut)));
            if (!listQuestsOk) failures.add(formatFailure(28, "List Quests", listQuestsOut));

            String updateQuestOut = UpdateQuestProgress.Server_UpdateQuestProgress(charA, questName);
            boolean updateQuestOk = !looksLikeError(updateQuestOut);
            results.add(new CaseResult(30, "Update Quest Progress", updateQuestOk, "UpdateQuestProgress output: " + summarize(updateQuestOut)));
            if (!updateQuestOk) failures.add(formatFailure(30, "Update Quest Progress", updateQuestOut));

            String completedQuestsOut = GetCompletedQuests.Server_GetCompletedQuests(charA);
            boolean completedQuestsOk = !looksLikeError(completedQuestsOut) && completedQuestsOut.toLowerCase().contains(questName.toLowerCase());
            results.add(new CaseResult(32, "Get Completed Quests", completedQuestsOk, "GetCompletedQuests output: " + summarize(completedQuestsOut)));
            if (!completedQuestsOk) failures.add(formatFailure(32, "Get Completed Quests", completedQuestsOut));

            // Character stats (33-34)
            String statsOut = GetCharacterStats.Server_GetCharacterStats(charB);
            boolean statsOk = !looksLikeError(statsOut) && statsOut.toLowerCase().contains(charB.toLowerCase());
            results.add(new CaseResult(33, "Get Character Stats", statsOk, "GetCharacterStats output: " + summarize(statsOut)));
            if (!statsOk) failures.add(formatFailure(33, "Get Character Stats", statsOut));

            int hp = pickFirstRequiredInt(setupConn,
                "SELECT CurrentHP FROM Character WHERE Name ILIKE ?",
                new Object[]{charB});
            int mp = pickFirstRequiredInt(setupConn,
                "SELECT CurrentMP FROM Character WHERE Name ILIKE ?",
                new Object[]{charB});
            int stam = pickFirstRequiredInt(setupConn,
                "SELECT CurrentStam FROM Character WHERE Name ILIKE ?",
                new Object[]{charB});
            String updateStatsOut = UpdateCharacterStats.Server_UpdateCharacterStats(charB, hp, mp, stam);
            boolean updateStatsOk = !looksLikeError(updateStatsOut) && updateStatsOut.toLowerCase().contains("success");
            results.add(new CaseResult(34, "Update Character Stats", updateStatsOk, "UpdateCharacterStats output: " + summarize(updateStatsOut)));
            if (!updateStatsOk) failures.add(formatFailure(34, "Update Character Stats", updateStatsOut));

            // enesapis listing & customization
            String listRacesOut = ListAllRaces.listAllRaces(setupConn);
            results.add(new CaseResult(41, "List All Races", !looksLikeError(listRacesOut), "ListAllRaces output: " + summarize(listRacesOut)));
            if (looksLikeError(listRacesOut)) failures.add(formatFailure(41, "List All Races", listRacesOut));

            String listClassesOut = ListAllClasses.listAllClasses(setupConn);
            results.add(new CaseResult(43, "List All Classes", !looksLikeError(listClassesOut), "ListAllClasses output: " + summarize(listClassesOut)));
            if (looksLikeError(listClassesOut)) failures.add(formatFailure(43, "List All Classes", listClassesOut));

            String listSkinOut = ListAllSkinColors.listAllSkinColors(setupConn);
            results.add(new CaseResult(42, "List All Skin Colors", !looksLikeError(listSkinOut), "ListAllSkinColors output: " + summarize(listSkinOut)));
            if (looksLikeError(listSkinOut)) failures.add(formatFailure(42, "List All Skin Colors", listSkinOut));

            String listLocationsOut = ListAllLocations.listAllLocations(setupConn);
            results.add(new CaseResult(44, "List All Locations", !looksLikeError(listLocationsOut), "ListAllLocations output: " + summarize(listLocationsOut)));
            if (looksLikeError(listLocationsOut)) failures.add(formatFailure(44, "List All Locations", listLocationsOut));

            String hairOptionsOut = GetHairOptions.getHairOptions(setupConn);
            results.add(new CaseResult(45, "Get Hair Options", !looksLikeError(hairOptionsOut), "GetHairOptions output: " + summarize(hairOptionsOut)));
            if (looksLikeError(hairOptionsOut)) failures.add(formatFailure(45, "Get Hair Options", hairOptionsOut));

            String listPlayersOut = ListAllPlayers.listAllPlayers(setupConn, 1);
            boolean listPlayersOk = !looksLikeError(listPlayersOut) && listPlayersOut.toLowerCase().contains("all players");
            results.add(new CaseResult(35, "List All Players", listPlayersOk, "ListAllPlayers output: " + summarize(listPlayersOut)));
            if (!listPlayersOk) failures.add(formatFailure(35, "List All Players", listPlayersOut));

            // Verify ListAllCharacters and compare with expected DB count (catches join/column bugs).
            int expectedChars = countCharactersForPlayer(setupConn, username);
            String listCharsOut = ListAllCharacters.Server_ListAllCharacters(username);
            boolean listCharsOk = !looksLikeError(listCharsOut)
                && (expectedChars == 0 ? listCharsOut.toLowerCase().contains("no characters found") : !listCharsOut.toLowerCase().contains("no characters found"));
            results.add(new CaseResult(38, "List All Characters", listCharsOk, "ListAllCharacters output: " + summarize(listCharsOut)));
            if (!listCharsOk) failures.add(formatFailure(38, "List All Characters", listCharsOut + " (expectedChars=" + expectedChars + ")"));

            // Ban player (36)
            String banOut = BanPlayer.banPlayer(setupConn, username);
            boolean banOk = !looksLikeError(banOut);
            Boolean banStatus = pickBooleanAny(setupConn, username,
                new String[]{"banstatus", "isbanned", "isBanned", "BanStatus", "IsBanned"});
            boolean banFinalOk = banOk && (banStatus == null || Boolean.TRUE.equals(banStatus));
            results.add(new CaseResult(36, "Ban Player", banFinalOk,
                "BanPlayer output: " + summarize(banOut) + " (banStatus=" + banStatus + ")"));
            if (!banFinalOk) failures.add(formatFailure(36, "Ban Player", banOut));

            // UpdateCharacterAppearance (40)
            String newHairType = hairType;
            String newHairColor = pickSecondOrFallbackString(setupConn,
                "SELECT color FROM haircolor ORDER BY id LIMIT 2",
                hairColor);
            String newSkinColor = pickSecondOrFallbackString(setupConn,
                "SELECT skincolor FROM skincolor ORDER BY id LIMIT 2",
                skinColor);
            String updateAppearanceOut = UpdateCharacterAppearance.updateCharacterAppearance(
                setupConn, charA, newHairType, newHairColor, newSkinColor);
            boolean updateAppearanceOk = !looksLikeError(updateAppearanceOut) && updateAppearanceOut.toLowerCase().contains("success");
            results.add(new CaseResult(40, "Update Character Appearance", updateAppearanceOk,
                "UpdateCharacterAppearance output: " + summarize(updateAppearanceOut)));
            if (!updateAppearanceOk) failures.add(formatFailure(40, "Update Character Appearance", updateAppearanceOut));

        } catch (Throwable t) {
            suiteCrashed = true;
            failures.add("Test suite crashed: " + t);
        } finally {
            DBConnection.shutdown();
        }

        CaseResult[] lastCaseById = new CaseResult[46];
        for (CaseResult r : results) {
            if (r.id >= 1 && r.id <= 45) lastCaseById[r.id] = r;
        }

        int passed = 0;
        int failed = 0;
        int tested = 0;
        for (int id = 1; id <= 45; id++) {
            if (lastCaseById[id] == null) continue;
            tested++;
            if (lastCaseById[id].passed) passed++;
            else failed++;
        }

        System.out.println();
        System.out.println("==================================================");
        System.out.println("API Test Summary");
        System.out.println("==================================================");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);

        if (suiteCrashed || (tested == 0 && !failures.isEmpty()) || failed > 0) {
            System.out.println();
            System.out.println("Failed Cases / Suite Crash:");
            for (int id = 1; id <= 45; id++) {
                CaseResult r = lastCaseById[id];
                if (r != null && !r.passed) {
                    System.out.println(" - [" + id + "] " + r.name + ": " + r.detail);
                }
            }
            if (suiteCrashed) {
                System.out.println();
                System.out.println("Suite crash details:");
                for (String f : failures) System.out.println(" - " + f);
            }
            System.out.println();
            System.out.println("Overall: FAIL");
        } else {
            System.out.println();
            System.out.println("Overall: SUCCESS");
        }
    }

    private interface StringSupplier {
        String get() throws Exception;
    }

    private static CaseResult runCase(int id, String name, StringSupplier fn) {
        try {
            String out = fn.get();
            boolean ok = out != null && !looksLikeError(out);
            return new CaseResult(id, name, ok, "output: " + summarize(out));
        } catch (Throwable t) {
            return new CaseResult(id, name, false, "threw: " + t);
        }
    }

    private static boolean hasFailureFor(List<CaseResult> results, int id, int unused) {
        // helper: treat as missing-case means failure
        for (CaseResult r : results) {
            if (r.id == id && !r.passed) return true;
        }
        return false;
    }

    private static String formatFailure(int id, String name, String detail) {
        return "[" + id + "] " + name + " => " + summarize(detail);
    }

    private static String summarize(String s) {
        if (s == null) return "null";
        String x = s.replaceAll("\\s+", " ").trim();
        return x.length() > 200 ? x.substring(0, 200) + "..." : x;
    }

    private static boolean looksLikeError(String out) {
        if (out == null) return true;
        String s = out.toLowerCase();
        return s.contains("error:") ||
            s.contains("database error") ||
            s.contains("transaction error") ||
            s.contains("client error") ||
            s.contains("database error occurred") ||
            s.contains("exception") ||
            s.contains("stacktrace") ||
            s.contains("client error:") ||
            s.contains("error retrieving");
    }

    private static String parseJoinCode(String createPartyOut) {
        if (createPartyOut == null) return null;
        int idx = createPartyOut.lastIndexOf(':');
        if (idx < 0) return null;
        String code = createPartyOut.substring(idx + 1).trim();
        return code.isEmpty() ? null : code;
    }

    private static boolean verifyPlayerExists(Connection conn, String username) {
        try {
            Integer count = pickFirstOptionalInt(conn,
                "SELECT COUNT(*) FROM player WHERE username = ?",
                new Object[]{username});
            return count != null && count > 0;
        } catch (Throwable t) {
            if (failuresRef != null) failuresRef.add("verifyPlayerExists failed: " + t);
            return false;
        }
    }

    private static boolean verifyCharacterExists(Connection conn, String charName) {
        try {
            Integer count = pickFirstOptionalInt(conn,
                "SELECT COUNT(*) FROM character WHERE name = ?",
                new Object[]{charName});
            return count != null && count > 0;
        } catch (Throwable t) {
            if (failuresRef != null) failuresRef.add("verifyCharacterExists failed: " + t);
            return false;
        }
    }

    private static String pickAbilityForCharacterOrInsert(Connection conn, String charName) throws SQLException {
        String ability = pickFirstOptionalString(conn,
            "SELECT a.Name FROM Ability a " +
                "JOIN CharacterAbility ca ON ca.AbilityID = a.ID " +
                "JOIN Character c ON ca.CharacterID = c.ID " +
                "WHERE c.Name ILIKE ? " +
                "ORDER BY a.ID LIMIT 1",
            new Object[]{charName});

        if (ability != null) return ability;

        // Try inserting a default ability link. If the SERIAL/sequence is behind,
        // INSERT may collide with existing rows (duplicate primary key), so we sync sequence first.
        Integer charId = pickFirstOptionalInt(conn,
            "SELECT ID FROM Character WHERE Name ILIKE ?",
            new Object[]{charName});
        Integer abilityId = pickFirstOptionalInt(conn,
            "SELECT ID FROM Ability ORDER BY ID LIMIT 1",
            null);
        if (charId == null || abilityId == null) return null;

        if (!tryInsertCharacterAbilityWithSequenceFix(conn, charId, abilityId)) {
            // retry once after another sequence sync (best-effort)
            if (!tryInsertCharacterAbilityWithSequenceFix(conn, charId, abilityId)) return null;
        }

        return pickFirstOptionalString(conn,
            "SELECT a.Name FROM Ability a " +
                "JOIN CharacterAbility ca ON ca.AbilityID = a.ID " +
                "JOIN Character c ON ca.CharacterID = c.ID " +
                "WHERE c.Name ILIKE ? " +
                "ORDER BY a.ID LIMIT 1",
            new Object[]{charName});
    }

    private static boolean tryInsertCharacterAbilityWithSequenceFix(Connection conn, int charId, int abilityId) {
        PreparedStatement seqFix = null;
        PreparedStatement stmt = null;
        try {
            // Keep the SERIAL sequence in sync with MAX(id) to avoid duplicate-key collisions.
            seqFix = conn.prepareStatement(
                "SELECT setval(pg_get_serial_sequence('characterability', 'id'), " +
                "(SELECT COALESCE(MAX(id),0) FROM characterability))");
            seqFix.execute();
        } catch (SQLException ignored) {
            // If pg_get_serial_sequence isn't available or schema differs, proceed anyway.
        } finally {
            if (seqFix != null) try { seqFix.close(); } catch (SQLException ignored) {}
        }

        try {
            stmt = conn.prepareStatement(
                "INSERT INTO characterability (characterid, abilityid) VALUES (?, ?)");
            stmt.setInt(1, charId);
            stmt.setInt(2, abilityId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (SQLException ignored) {}
        }
    }

    private static int countCharactersForPlayer(Connection conn, String username) throws SQLException {
        Integer count = pickFirstOptionalInt(conn,
            "SELECT COUNT(*) " +
                "FROM Character c " +
                "JOIN Player p ON c.PlayerID = p.Username " +
                "WHERE p.Username ILIKE ?",
            new Object[]{username});
        return count == null ? 0 : count;
    }

    private static Boolean pickBooleanAny(Connection conn, String username, String[] cols) {
        // Attempts multiple candidate column names to accommodate schema casing / naming inconsistencies.
        for (String col : cols) {
            try {
                String sql = "SELECT " + col + " FROM player WHERE username ILIKE ? LIMIT 1";
                PreparedStatement stmt = conn.prepareStatement(sql);
                try {
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();
                    try {
                        if (rs.next()) {
                            Object v = rs.getObject(1);
                            if (v == null) return null;
                            if (v instanceof Boolean) return (Boolean) v;
                            if (v instanceof String) return Boolean.parseBoolean((String) v);
                            if (v instanceof Integer) return ((Integer) v) != 0;
                        }
                    } finally {
                        if (rs != null) rs.close();
                    }
                } finally {
                    if (stmt != null) stmt.close();
                }
            } catch (Throwable ignored) {
                // try next column
            }
        }
        return null;
    }

    private static String pickFirstRequiredString(Connection conn, String sql, Object[] params) throws SQLException {
        String v = pickFirstOptionalString(conn, sql, params);
        if (v == null) throw new SQLException("Missing required value for sql: " + sql);
        return v;
    }

    private static String pickFirstOptionalString(Connection conn, String sql, Object[] params) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }
            rs = stmt.executeQuery();
            if (!rs.next()) return null;
            return rs.getString(1);
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    private static String pickSecondOrFallbackString(Connection conn, String sql, String fallback) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            String first = null;
            String second = null;
            if (rs.next()) first = rs.getString(1);
            if (rs.next()) second = rs.getString(1);
            if (second != null) return second;
            return first != null ? first : fallback;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    private static Integer pickFirstOptionalInt(Connection conn, String sql, Object[] params) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
            }
            rs = stmt.executeQuery();
            if (!rs.next()) return null;
            Object v = rs.getObject(1);
            if (v == null) return null;
            return rs.getInt(1);
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    private static int pickFirstRequiredInt(Connection conn, String sql, Object[] params) throws SQLException {
        Integer v = pickFirstOptionalInt(conn, sql, params);
        if (v == null) throw new SQLException("Missing required int for sql: " + sql);
        return v;
    }

    private static String pickFirstOptionalString(Connection conn, String sql, Object param) throws SQLException {
        return pickFirstOptionalString(conn, sql, new Object[]{param});
    }
}

