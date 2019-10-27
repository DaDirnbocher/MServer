package de.mediathekview.mserver.base.webaccess;

import java.io.IOException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

/**
 * Helper Class to get rid of static method call for better testability
 */
public class JsoupConnection {

  public Connection getConnection(String url) throws IOException {
    return Jsoup.connect(url);
  }

  public Document getDocument(String url) throws IOException {
    return Jsoup.connect(url).get();
  }

  public Document getDocumentTimeoutAfter(String url, int timeoutInMilliseconds) throws IOException {
    return Jsoup.connect(url).timeout(timeoutInMilliseconds).get();
  }

  public Document getDocumentTimeoutAfterAlternativeDocumentType(String url, int timeoutInMilliseconds, Parser parser) throws IOException {
    return Jsoup.connect(url).timeout(timeoutInMilliseconds).parser(parser).get();
  }

}
