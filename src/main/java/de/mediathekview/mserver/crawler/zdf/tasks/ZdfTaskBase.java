package de.mediathekview.mserver.crawler.zdf.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.mediathekview.mserver.crawler.basic.AbstractCrawler;
import de.mediathekview.mserver.crawler.basic.AbstractRestTask;
import de.mediathekview.mserver.crawler.basic.CrawlerUrlDTO;
import de.mediathekview.mserver.crawler.zdf.ZdfConstants;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ZdfTaskBase<T, D extends CrawlerUrlDTO> extends AbstractRestTask<T, D> {

  private static final Logger LOG = LogManager.getLogger(ZdfTaskBase.class);

  private final GsonBuilder gsonBuilder;

  public ZdfTaskBase(AbstractCrawler aCrawler,
      ConcurrentLinkedQueue<D> aUrlToCrawlDtos, Optional<String> aAuthKey) {
    super(aCrawler, aUrlToCrawlDtos, aAuthKey);
    gsonBuilder = new GsonBuilder();
  }

  protected void registerJsonDeserializer(final Type aType, final Object aDeserializer) {
    gsonBuilder.registerTypeAdapter(aType, aDeserializer);
  }

  protected <T> Optional<T> deserializeOptional(final WebTarget aTarget, final Type aType) {

    final Gson gson = gsonBuilder.create();
    final Response response = executeRequest(aTarget);
    if (response.getStatus() == 200) {
      final String jsonOutput = response.readEntity(String.class);
      return gson.fromJson(jsonOutput, aType);
    } else {
      LOG.error("ZdfTaskBase: request of url " + aTarget.getUri().toString() + " failed: " + response.getStatus());
    }

    return Optional.empty();
  }

  protected <T> T deserialize(final WebTarget aTarget, final Type aType) {

    final Gson gson = gsonBuilder.create();
    final Response response = executeRequest(aTarget);
    if (response.getStatus() == 200) {
      final String jsonOutput = response.readEntity(String.class);
      return gson.fromJson(jsonOutput, aType);
    } else {
      LOG.error("ZdfTaskBase: request of url " + aTarget.getUri().toString() + " failed: " + response.getStatus());
    }

    return null;
  }

  private Response executeRequest(final WebTarget aTarget) {
    Builder request = aTarget.request();
    if (authKey.isPresent()) {
      request = request.header(ZdfConstants.HEADER_AUTHENTIFICATION, AUTHORIZATION_BEARER + authKey.get());
    }

    return request.header(HEADER_ACCEPT_ENCODING, ENCODING_GZIP).get();
  }
}
