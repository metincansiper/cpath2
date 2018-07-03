package cpath.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static cpath.service.Status.*;

import cpath.service.args.ServiceQuery;
import cpath.service.args.Search;
import cpath.service.args.TopPathways;
import cpath.service.args.Traverse;
import cpath.service.jaxb.*;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * Basic controller.
 *
 * @author rodche
 */
public abstract class BasicController {
  private static final Logger log = LoggerFactory.getLogger(BasicController.class);

  protected CPathService service;

  @Autowired
  public void setLogRepository(CPathService service) {
    Assert.notNull(service, "'service' was null");
    this.service = service;
  }

  /**
   * Http error response with more details and specific access log events.
   *
   * @param args
   * @param error
   * @param request
   * @param response
   */
  protected final void errorResponse(ServiceQuery args, ErrorResponse error,
                                     HttpServletRequest request, HttpServletResponse response) {
    try {
      //log/track using a shorter message
      track(request, args, null, error);
      //return a long detailed message
      response.sendError(error.getStatus().getCode(), error.getStatus().getCode() + "; " + error.toString());
    } catch (IOException e) {
      log.error("Problem sending back an error response; " + e);
    }
  }


  /**
   * Builds an error message from
   * the web parameters binding result
   * if there're errors.
   *
   * @param bindingResult
   * @return
   */
  protected final String errorFromBindingResult(BindingResult bindingResult) {
    StringBuilder sb = new StringBuilder();
    for (FieldError fe : bindingResult.getFieldErrors()) {
      Object rejectedVal = fe.getRejectedValue();
      if (rejectedVal instanceof Object[]) {
        if (((Object[]) rejectedVal).length > 0) {
          rejectedVal = Arrays.toString((Object[]) rejectedVal);
        } else {
          rejectedVal = "empty array";
        }
      }
      sb.append(fe.getField() + " was '" + rejectedVal + "'; "
        + fe.getDefaultMessage() + ". ");
    }

    return sb.toString();
  }


  /**
   * Writes the query results to the HTTP response
   * output stream.
   *
   * @param args     query args
   * @param result
   * @param request
   * @param response
   */
  protected final void stringResponse(ServiceQuery args, ServiceResponse result,
                                      HttpServletRequest request, HttpServletResponse response)
  {
    if (result instanceof ErrorResponse) {
      errorResponse(args, (ErrorResponse) result, request, response);
    } else if (result instanceof DataResponse) {
      final DataResponse dataResponse = (DataResponse) result;

      // log/track one data access event for each data provider listed in the result
      track(request, args, dataResponse.getProviders(), null);

      if (dataResponse.getData() instanceof Path) {
        //get the temp file
        Path resultFile = (Path) dataResponse.getData();
        try {
          response.setContentType(dataResponse.getFormat().getMediaType() + "; charset=utf-8");
          long size = Files.size(resultFile);
          if (size > 13) { // a hack to skip for trivial/empty results
            Files.copy(resultFile, response.getOutputStream());
          }
        } catch (IOException e) {
          String msg = String.format("Failed to process the (temporary) result file %s; %s.",
            resultFile, e.toString());
          errorResponse(args, new ErrorResponse(INTERNAL_ERROR, msg), request, response);
        } finally {
          try {
            Files.delete(resultFile);
          } catch (Exception e) {
            log.error(e.toString());
          }
        }
      } else if (dataResponse.isEmpty()) {
        //return empty string or trivial valid RDF/XML
        response.setContentType(dataResponse.getFormat().getMediaType());
        try {
          if (dataResponse.getFormat() == OutputFormat.BIOPAX) {
            //output an empty trivial BioPAX model
            Model emptyModel = BioPAXLevel.L3.getDefaultFactory().createModel();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            new SimpleIOHandler().convertToOWL(emptyModel, bos);
            response.getWriter().print(bos.toString("UTF-8"));
          } else {
            ;//SIF,GSEA formats do not allow comments
          }
        } catch (IOException e) {
          String msg = String.format("Failed writing a trivial response: %s.", e.toString());
          errorResponse(args, new ErrorResponse(INTERNAL_ERROR, msg), request, response);
        }
      } else { //it's probably a bug -
        String msg = String.format("BUG: DataResponse.data has value: %s, %s instead of a Path or null.",
          dataResponse.getData().getClass().getSimpleName(), dataResponse.toString());
        errorResponse(args, new ErrorResponse(INTERNAL_ERROR, msg), request, response);
      }
    } else { //it's a bug -
      String msg = String.format("BUG: Unknown ServiceResponse: %s, %s ",
        result.getClass().getSimpleName(), result.toString());
      errorResponse(args, new ErrorResponse(INTERNAL_ERROR, msg), request, response);
    }
  }


  /**
   * Resizes the image.
   *
   * @param img
   * @param width
   * @param height
   * @param background
   * @return
   */
  public final BufferedImage scaleImage(BufferedImage img, int width, int height,
                                        Color background) {
    int imgWidth = img.getWidth();
    int imgHeight = img.getHeight();
    if (imgWidth * height < imgHeight * width) {
      width = imgWidth * height / imgHeight;
    } else {
      height = imgHeight * width / imgWidth;
    }
    BufferedImage newImage = new BufferedImage(width, height,
      BufferedImage.TYPE_INT_RGB);
    Graphics2D g = newImage.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      if (background != null)
        g.setBackground(background);
      g.clearRect(0, 0, width, height);
      g.drawImage(img, 0, 0, width, height, null);
    } finally {
      g.dispose();
    }
    return newImage;
  }


  /**
   * Extracts the client's IP from the request headers.
   *
   * @param request
   * @return
   */
  public static final String clientIpAddress(HttpServletRequest request) {

    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }

    return ip;
  }


  // data access logging and tracking
  void track(HttpServletRequest request, ServiceQuery args, Set<String> providers, ErrorResponse err) {
    final String ip = clientIpAddress(request);

    String client = args.getUser();
    if (client == null || client.isEmpty()) {
      //extract http client tool name/version part:
      client = request.getHeader("User-Agent");
      if (client != null && !client.isEmpty() && client.contains(" ")) {
        client = client.substring(0, client.indexOf(" "));
      }
    }

    Integer status = 200;
    if (err != null) {
      status = err.getErrorCode();
      service.track(ip, "error", err.getErrorMsg());
    }

		service.track(ip, "command", args.cmd());

    service.track(ip, "client", client);

    String msg = status + ": " + args.toString();
    if (providers != null) {
      for (String provider : providers) service.track(ip, "provider", provider);
      if (!providers.isEmpty()) msg += "; pro:" + String.join(",", providers);
    }

    service.track(ip, "all", msg);

    String f = args.outputFormat().toLowerCase();
    if (args instanceof Search || args instanceof TopPathways || args instanceof Traverse)
    { //it's json or xml (URI extension based content negotiation is favored over the request header based)
      String h = String.valueOf(request.getHeader("accept"));
      if(request.getRequestURI().endsWith(".xml"))
        f = "xml";
      else if(request.getRequestURI().endsWith(".json"))
        f = "json";
      else if(h.equals("null") || h.isEmpty() || h.contains("application/json"))
        f = "json"; //default
      else if(h.contains("application/xml"))
        f = "xml";
    }
    service.track(ip, "format", f);
  }
}