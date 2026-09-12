package com.example.data.api

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class VenueDto(
    val id: Int,
    val name: String,
    val city: String?,
    val capacity: Int?
)

@JsonClass(generateAdapter = true)
data class TeamDto(
    val id: Int,
    val name: String,
    val shortName: String,
    val acronym: String?,
    val logoUrl: String?,
    val foundedYear: Int? = null,
    val country: String? = null,
    val venue: VenueDto? = null
)

@JsonClass(generateAdapter = true)
data class MatchDto(
    val id: Int,
    val round: String?,
    val kickoffTime: String?,
    val status: String?,
    val homeScore: Int?,
    val awayScore: Int?,
    val homeTeam: TeamDto,
    val awayTeam: TeamDto,
    val league: String? = null
)

@JsonClass(generateAdapter = true)
data class StandingDto(
    val position: Int,
    val team: TeamDto,
    val points: Int,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val form: String?
)

@JsonClass(generateAdapter = true)
data class StandingsResponse(
    val seasonId: Int,
    val standings: List<StandingDto>
)


@JsonClass(generateAdapter = true)
data class PlayerBasicDto(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class MatchEventDto(
    val id: Int,
    val minute: Int,
    val extraMinute: Int?,
    val type: String,
    val description: String?,
    val teamId: Int,
    val player: PlayerBasicDto?,
    val relatedPlayer: PlayerBasicDto?
)
interface BrasaFutApi {
    @GET("api/v1/matches")
    suspend fun getMatches(@Header("x-api-key") apiKey: String): List<MatchDto>

    @GET("api/v1/matches/{id}")
    suspend fun getMatch(@Path("id") matchId: Int, @Header("x-api-key") apiKey: String): MatchDto
    @GET("api/v1/matches/{id}/events")
    suspend fun getMatchEvents(@Path("id") matchId: Int, @Header("x-api-key") apiKey: String): List<MatchEventDto>


    @GET("api/v1/matches/live")
    suspend fun getLiveMatches(@Header("x-api-key") apiKey: String): List<MatchDto>

    @GET("api/v1/matches/head-to-head")
    suspend fun getHeadToHead(
        @Query("team1Id") team1Id: Int,
        @Query("team2Id") team2Id: Int,
        @Header("x-api-key") apiKey: String
    ): List<MatchDto>

    @GET("api/v1/teams")
    suspend fun searchTeams(@Header("x-api-key") apiKey: String, @Query("search") query: String): List<TeamDto>

    @GET("api/v1/standings")
    suspend fun getStandings(@Query("seasonId") seasonId: Int, @Header("x-api-key") apiKey: String): StandingsResponse
}
