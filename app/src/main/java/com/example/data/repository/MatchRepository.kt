package com.example.data.repository

import com.example.data.api.BrasaFutApi
import com.example.data.api.MatchDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MatchRepository {
    // Enterprise API Key - 1000 req/min limit
    private val apiKey = "bf_live_enterprise_9f83a21c45e87b60d4e92a11bf738e45"
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val api: BrasaFutApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
            
        Retrofit.Builder()
            .baseUrl("https://api-brasa-fut.vercel.app/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BrasaFutApi::class.java)
    }
    
    private val cacheDao by lazy {
        com.example.data.local.AppDatabase.getDatabase(com.example.BrasaFutApplication.appContext).cacheDao()
    }
    
    private val matchListAdapter = moshi.adapter<List<MatchDto>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, MatchDto::class.java)
    )
    private val matchAdapter = moshi.adapter(MatchDto::class.java)
    private val eventListAdapter = moshi.adapter<List<com.example.data.api.MatchEventDto>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.api.MatchEventDto::class.java)
    )
    private val standingsListAdapter = moshi.adapter<List<com.example.data.api.StandingDto>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.api.StandingDto::class.java)
    )

    private suspend fun <T> withCache(key: String, fetch: suspend () -> T, adapter: com.squareup.moshi.JsonAdapter<T>): T {
        return try {
            val result = fetch()
            cacheDao.insertCache(com.example.data.local.CacheEntity(key, adapter.toJson(result), System.currentTimeMillis()))
            result
        } catch (e: Exception) {
            val cached = cacheDao.getCache(key)
            if (cached != null) {
                adapter.fromJson(cached.jsonPayload) ?: throw e
            } else {
                throw e
            }
        }
    }

    suspend fun getMatches(): List<MatchDto> {
        return withCache("matches_all", { api.getMatches(apiKey) }, matchListAdapter)
    }

    suspend fun getMatch(id: Int): MatchDto {
        return withCache("match_$id", { api.getMatch(id, apiKey) }, matchAdapter)
    }

    suspend fun getMatchEvents(id: Int): List<com.example.data.api.MatchEventDto> {
        return withCache("match_events_$id", { api.getMatchEvents(id, apiKey) }, eventListAdapter)
    }

    suspend fun getLiveMatches(): List<MatchDto> {
        // We probably shouldn't cache live matches for long, but offline mode is offline mode.
        return withCache("matches_live", { api.getLiveMatches(apiKey) }, matchListAdapter)
    }

    suspend fun getHeadToHead(team1Id: Int, team2Id: Int): List<MatchDto> {
        return withCache("h2h_${team1Id}_$team2Id", { api.getHeadToHead(team1Id, team2Id, apiKey) }, matchListAdapter)
    }

    suspend fun searchTeams(query: String): List<com.example.data.api.TeamDto> {
        val adapter = moshi.adapter<List<com.example.data.api.TeamDto>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.api.TeamDto::class.java)
        )
        return withCache("search_team_$query", { api.searchTeams(apiKey, query) }, adapter)
    }

    suspend fun getStandings(seasonId: Int = 1): List<com.example.data.api.StandingDto> {
        return withCache("standings_$seasonId", { api.getStandings(seasonId, apiKey).standings }, standingsListAdapter)
    }
}