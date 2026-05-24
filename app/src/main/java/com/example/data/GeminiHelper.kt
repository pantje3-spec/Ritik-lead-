package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiHelper {
    private const val TAG = "GeminiHelper"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Attempts to read the GEMINI_API_KEY fromBuildConfig.
     * Returns null or empty if not present.
     */
    val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

    val isKeyConfigured: Boolean
        get() = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

    /**
     * Generates a tailor-made social media advertising pitch for the lead.
     */
    suspend fun generateSmmPitch(
        leadName: String,
        businessType: String,
        source: String,
        budget: Double,
        priority: String
    ): String = withContext(Dispatchers.IO) {
        if (!isKeyConfigured) {
            return@withContext getLocalFallbackPitch(leadName, businessType, source, budget, priority)
        }

        val prompt = """
            You are a growth marketing and SMM specialist. Generate a professional, short, conversion-focused WhatsApp message outreach copy for a client lead.
            Details:
            - Client Name: $leadName
            - Business Type: $businessType
            - Lead Source: $source
            - Target SMM Monthly Ad Budget: $budget rupees
            - Lead Priority: $priority
            
            Keep the tone clean, inviting, highly persuasive, and customized to their niche. Use 3-4 natural bullet points telling them how we'll scale their sales (such as Meta/Instagram retargeting ads, premium visual creative hooks, high conversion landing pages, double-funnel attribution).
            Include a warm call to action asking for a brief Zoom audit. Keep the total message under 150 words. Do not use generic placeholders.
        """.trimIndent()

        try {
            val responseText = makeApiCall(prompt)
            if (responseText.isNotEmpty()) {
                return@withContext responseText
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API failed, falling back to local template", e)
        }

        return@withContext getLocalFallbackPitch(leadName, businessType, source, budget, priority)
    }

    /**
     * Evaluates a lead's growth prospects and provides an SMM marketing recommendation.
     * Returns a pair of: Pair(Score: Int, GrowthRecommendation: String)
     */
    suspend fun analyzeLeadAndScore(
        leadName: String,
        businessType: String,
        source: String,
        budget: Double,
        adCampaign: String
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        // Calculate a smart dynamic local baseline score
        var baseScore = 60
        if (budget >= 50000) baseScore += 20
        else if (budget >= 25000) baseScore += 10
        if (source.contains("Ads", ignoreCase = true) || source.contains("Search", ignoreCase = true)) baseScore += 12
        if (adCampaign.isNotEmpty()) baseScore += 8
        if (baseScore > 98) baseScore = 98

        if (!isKeyConfigured) {
            val localRecommendation = getLocalFallbackAnalysis(leadName, businessType, source, budget, adCampaign)
            return@withContext Pair(baseScore, localRecommendation)
        }

        val prompt = """
            Analyze this Social Media Marketing (SMM) agency lead.
            Details:
            - Business Type: $businessType
            - Lead Source: $source
            - Trial Monthly Budget: $budget rupees
            - Ad Campaign: $adCampaign
            
            Provide an SMM Lead Audit Report in JSON format.
            You MUST return ONLY a JSON object containing EXACTLY these two keys:
            - "score": an integer representing the ROI qualification score (between 30 and 100).
            - "analysis": a 2-3 sentence strategic recommendation explaining:
               1. Their target audience on social media (e.g. Meta for visual vs LinkedIn for B2B)
               2. First action step we should pitch them (e.g. Lead Gen Ads, Retargeting)
               3. Suggested campaign angle.
            
            Example Format:
            {
               "score": 85,
               "analysis": "This local dental chain has strong visual appeal suited for Meta Lead Form campaigns. We should pitch a localized discount lead capture funnel with testimonial-focused ad assets to maximize conversion."
            }
            Do not include backticks or markdown markers. Just return the raw JSON object.
        """.trimIndent()

        try {
            val jsonResponseStr = makeApiCall(prompt)
            // Strip markdown formatting if any present
            val cleanedStr = jsonResponseStr.replace("```json", "").replace("```", "").trim()
            val json = JSONObject(cleanedStr)
            val score = json.optInt("score", baseScore)
            val analysis = json.optString("analysis", getLocalFallbackAnalysis(leadName, businessType, source, budget, adCampaign))
            return@withContext Pair(score, analysis)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini analysis API failed, using smart local analyzer", e)
            val localRecommendation = getLocalFallbackAnalysis(leadName, businessType, source, budget, adCampaign)
            return@withContext Pair(baseScore, localRecommendation)
        }
    }

    private fun makeApiCall(prompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        // Build request body according to API spec
        val requestJson = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected response code: ${response.code}")
            val responseBody = response.body?.string() ?: throw Exception("Empty response body")
            
            // Parse response json
            val resObj = JSONObject(responseBody)
            val candidates = resObj.getJSONArray("candidates")
            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            return parts.getJSONObject(0).getString("text")
        }
    }

    private fun getLocalFallbackPitch(
        leadName: String,
        businessType: String,
        source: String,
        budget: Double,
        priority: String
    ): String {
        val cleanBud = String.format("₹%,.0f", budget)
        return """
            Hello $leadName, thank you for showing interest in our SMM growth services! 🚀
            
            We reviewed your inquiry for $businessType (via $source) and engineered a high-converting growth outline for your level:
            
            • Targeted Facebook & Instagram Lead-Capture Ads to map $businessType shoppers.
            • Dynamic Retargeting to convert interested site visitors into paid appointments.
            • Content Hook Optimization to double engagement rate on organic reels.
            
            We estimated a scalable pilot budget of $cleanBud of ads-spend for maximum ROI.
            
            Let's schedule a brief 10-minute Zoom audit tomorrow to outline your brand road-map. Would morning or afternoon work best?
        """.trimIndent()
    }

    private fun getLocalFallbackAnalysis(
        leadName: String,
        businessType: String,
        source: String,
        budget: Double,
        adCampaign: String
    ): String {
        val channelStr = if (source.contains("LinkedIn")) "LinkedIn Professional Outreach" else "Meta Visual Lead Forms & Instagram Reels"
        val urgencyStr = if (budget >= 40000) "Highly profitable high-intent scaling." else "Standard local micro-targeting."
        return "This $businessType lead has excellent potential on $channelStr campaigns. Recommended first pitch is an engagement retargeting funnel focusing on case-studies, coupled with localized ad placements. Budget is adequate to trigger initial high-intent optimizations. $urgencyStr"
    }
}
