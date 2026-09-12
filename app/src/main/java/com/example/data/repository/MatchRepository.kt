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
    
    private val api: BrasaFutApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
            
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://api-brasa-fut.vercel.app/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BrasaFutApi::class.java)
    }

    suspend fun getMatches(): List<MatchDto> {
        return api.getMatches(apiKey)
    }

    suspend fun getMatch(id: Int): MatchDto {
        return api.getMatch(id, apiKey)
    }

    suspend fun getMatchEvents(id: Int): List<com.example.data.api.MatchEventDto> {
        return api.getMatchEvents(id, apiKey)
    }

    suspend fun getLiveMatches(): List<MatchDto> {
        return api.getLiveMatches(apiKey)
    }

    suspend fun getHeadToHead(team1Id: Int, team2Id: Int): List<MatchDto> {
        return api.getHeadToHead(team1Id, team2Id, apiKey)
    }

    suspend fun searchTeams(query: String): List<com.example.data.api.TeamDto> {
        return api.searchTeams(apiKey, query)
    }

    suspend fun getStandings(seasonId: Int = 1): List<com.example.data.api.StandingDto> {
        return api.getStandings(seasonId, apiKey).standings
    }
}
