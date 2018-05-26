package ca.mcgill.sus.screensaver

import ca.mcgill.science.tepid.utils.LogUtils
import ca.mcgill.science.tepid.utils.PropsLDAP
import ca.mcgill.science.tepid.utils.PropsURL
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

    val DEBUG: Boolean

    init {

        log.info("**********************************")
        log.info("*       Setting up Configs       *")
        log.info("**********************************")


        SERVER_URL = PropsURL.SERVER_URL_PRODUCTION  + "screensaver/"
        DEBUG = PropsURL.SERVER_URL_TESTING != SERVER_URL

        WEB_URL = PropsURL.WEB_URL_PRODUCTION

        if (DEBUG) LogUtils.setLoggingLevel(log, Level.TRACE)

    }

}