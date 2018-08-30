package de.mediathekview.mserver.crawler.arte.json;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import de.mediathekview.mserver.base.utils.JsonUtils;
import de.mediathekview.mserver.crawler.arte.ArteJsonElementDto;
import de.mediathekview.mserver.crawler.arte.tasks.ArteFilmListDTO;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;

public class ArteFilmListDeserializer implements JsonDeserializer<ArteFilmListDTO> {
  private static final Logger LOG = LogManager.getLogger(ArteFilmListDeserializer.class);
  private static final String PATTERN_PAGE_NUMBER = "page=\\d+";
  private static final String JSON_ELEMENT_HREF = "href";
  private static final String JSON_ELEMENT_NEXT = "next";
  private static final String JSON_ELEMENT_LINKS = "links";
  private static final String JSON_ELEMENT_VIDEOS = "videos";
  private static final String JSON_ELEMENT_META = "meta";
  private final AbstractCrawler crawler;
  private final Optional<String> subcategoryName;

  public ArteFilmListDeserializer(final AbstractCrawler aCrawler,
      final Optional<String> aSubcategoryName) {
    crawler = aCrawler;
    subcategoryName = aSubcategoryName;
  }

  @Override
  public ArteFilmListDTO deserialize(final JsonElement aJsonElement, final Type aType,
      final JsonDeserializationContext aJsonDeserializationContext) throws JsonParseException {
    final ArteFilmListDTO result = new ArteFilmListDTO();
    if (aJsonElement.isJsonObject()) {
      final JsonObject mainObj = aJsonElement.getAsJsonObject();
      result.setNextPage(getNextPageLink(mainObj));
      if (JsonUtils.checkTreePath(mainObj, Optional.of(crawler), getBaseElementName())) {
        mainObj.get(getBaseElementName()).getAsJsonArray().forEach(filmElement -> result
            .addFoundFilm(new ArteJsonElementDto(filmElement, subcategoryName)));
      }
    }
    return result;
  }

  private Optional<URI> getNextPageLink(final JsonObject mainObj) {
    if (JsonUtils.checkTreePath(mainObj, Optional.empty(), JSON_ELEMENT_META, getBaseElementName(),
        JSON_ELEMENT_LINKS, JSON_ELEMENT_NEXT, JSON_ELEMENT_HREF)) {

      final String nextPageUrl = mainObj.get(JSON_ELEMENT_META).getAsJsonObject()
          .get(getBaseElementName()).getAsJsonObject().get(JSON_ELEMENT_LINKS).getAsJsonObject()
          .get(JSON_ELEMENT_NEXT).getAsJsonObject().get(JSON_ELEMENT_HREF).getAsString();
      final Matcher nextPageNumberMatcher =
          Pattern.compile(PATTERN_PAGE_NUMBER).matcher(nextPageUrl);
      try {
        if (nextPageNumberMatcher.find()
            && Integer.parseInt(nextPageNumberMatcher.group()) <= crawler.getRuntimeConfig()
                .getMaximumSubpages()) {
          return Optional.of(new URI(nextPageUrl));
        }
      } catch (final NumberFormatException numberFormatException) {
        LOG.error("A page number can't be read.", numberFormatException);
      } catch (final URISyntaxException uriSyntaxException) {
        LOG.error("A next page URL isn't a valid URI.", uriSyntaxException);
      }

    }

    return Optional.empty();
  }

  protected String getBaseElementName() {
    return JSON_ELEMENT_VIDEOS;
  }

}