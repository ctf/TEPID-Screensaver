package ca.mcgill.sus.screensaver

import ca.mcgill.science.tepid.api.ITepidScreensaver
import ca.mcgill.science.tepid.api.TepidScreensaverApi
import ca.mcgill.science.tepid.utils.LogUtils
import ca.mcgill.science.tepid.utils.PropsScreensaver
import ca.mcgill.science.tepid.utils.PropsURL
import ca.mcgill.science.tepid.utils.WithLogging
import org.apache.logging.log4j.Level
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.util.concurrent.CompletableFuture

/**
 * A unified interface for all the config gathering.
 * Config options are pulled with the PropHolders in TEPID-Commons
 */
object Config : WithLogging() {

    val SERVER_URL: String
    val WEB_URL: String

    val office_regex: String
    val gravatar_search_terms: String
    val report_malfunctioning_to: String
    val announcement_slide_directory: String
    val background_picture_directory: String
    val GOOGLE_CUSTOM_SEARCH_KEY: String
    val ICS_CALENDAR_ADDRESS: String

    val DEBUG: Boolean

    init {

        log.info("**********************************")
        log.info("*       Setting up Configs       *")
        log.info("**********************************")


        SERVER_URL = PropsURL.SERVER_URL_PRODUCTION?.plus("screensaver/") ?: throw RuntimeException()
        DEBUG = PropsURL.TESTING?.toBoolean() ?: true

        WEB_URL = PropsURL.WEB_URL_PRODUCTION ?: throw RuntimeException()

        office_regex = PropsScreensaver.OFFICE_REGEX ?: ""
        gravatar_search_terms = PropsScreensaver.GRAVATAR_SEARCH_TERMS ?: ""
        report_malfunctioning_to = PropsScreensaver.REPORT_MALFUNCTIONING_COMPUTER_TEXT ?: ""
        announcement_slide_directory = PropsScreensaver.ANNOUNCEMENT_SLIDE_LOCATION ?: ""
        background_picture_directory = PropsScreensaver.BACKGROUND_PICTURE_LOCATION ?: ""
        GOOGLE_CUSTOM_SEARCH_KEY = PropsScreensaver.GOOGLE_CUSTOM_SEARCH_KEY ?: ""
        ICS_CALENDAR_ADDRESS = PropsScreensaver.ICS_CALENDAR_ADDRESS ?: ""

        if (DEBUG) LogUtils.setLoggingLevel(log, Level.TRACE)
    }
}

fun buildApi(): ITepidScreensaver {
    return TepidScreensaverApi(Config.SERVER_URL, Config.DEBUG).create()
}

val api = buildApi()

fun <R> Call<R>.asCompletableFuture(): CompletableFuture<R> {
    val future = object : CompletableFuture<R>() {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            if (mayInterruptIfRunning) {
                cancel()
            }
            return super.cancel(mayInterruptIfRunning)
        }
    }

    enqueue(object : Callback<R> {
        override fun onResponse(call: Call<R>, response: Response<R>) {
            if (response.isSuccessful) {
                future.complete(response.body())
            } else {
                future.completeExceptionally(HttpException(response))
            }
        }

        override fun onFailure(call: Call<R>, t: Throwable) {
            future.completeExceptionally(t)
        }
    })

    return future
}
