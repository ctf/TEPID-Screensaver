package ca.mcgill.sus.screensaver

import ca.mcgill.science.tepid.utils.LogUtils
import ca.mcgill.science.tepid.utils.PropsLDAP
import ca.mcgill.science.tepid.utils.PropsURL
import ca.mcgill.science.tepid.utils.PropsScreensaver
import ca.mcgill.science.tepid.utils.WithLogging
import org.apache.logging.log4j.Level

/**
 * Created by Allan Wang on 24/03/2018.
 *
 * The following are default keys used for testing
 * They are pulled from priv.properties under the root project folder
 * If no file is found, default values will be supplied (usually empty strings)
 */
object Config : WithLogging() {

    val SERVER_URL: String
    val WEB_URL: String

    val office_regex: String
    val gravatar_search_terms: String
    val report_malfunctioning_to: String

    val DEBUG: Boolean

    init {

        log.info("**********************************")
        log.info("*       Setting up Configs       *")
        log.info("**********************************")


        SERVER_URL = PropsURL.SERVER_URL_PRODUCTION  + "screensaver/"
        DEBUG = PropsURL.SERVER_URL_TESTING != SERVER_URL

        WEB_URL = PropsURL.WEB_URL_PRODUCTION

        office_regex = PropsScreensaver.OFFICE_REGEX
        gravatar_search_terms = PropsScreensaver.GRAVATAR_SEARCH_TERMS
        report_malfunctioning_to = PropsScreensaver.REPORT_MALFUNCTIONING_COMPUTER_TEXT


        if (DEBUG) LogUtils.setLoggingLevel(log, Level.TRACE)

    }

}