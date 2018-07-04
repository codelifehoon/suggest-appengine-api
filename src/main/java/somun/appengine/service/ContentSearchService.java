package somun.appengine.service;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.GeoPoint;
import com.google.appengine.api.search.Query;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchException;
import com.google.appengine.api.search.SortOptions;
import com.google.appengine.api.search.StatusCode;

import lombok.extern.slf4j.Slf4j;
import somun.appengine.AppengineUtils;
import somun.appengine.config.properties.SomunProperties;
import somun.appengine.common.util.DateUtils;
import somun.appengine.service.repository.SearchIndexComb;


@Slf4j
@Service
public class ContentSearchService {

    @Autowired
    SomunProperties somunProperties;


    static String searchIndexName = "totalSearchIndex_v1.2";


    public int mergeTotalSearchIndex(String Url) throws InterruptedException {

//

        RestTemplate restTemplate = new RestTemplate();
        PagedResources<SearchIndexComb> response = restTemplate.exchange(Url
            , HttpMethod.GET
            , null
            , new ParameterizedTypeReference<PagedResources<SearchIndexComb>>() {
            }).getBody();

        PagedResources.PageMetadata pageInfo = response.getMetadata();
        Collection<SearchIndexComb> searchIndexContents = response.getContent();

        List<Document> documents = searchIndexContents.stream().map((d) -> {


//            log.debug("documents"+ d.toString());
            GeoPoint geoPoint = null;

            if  (d.getLatitude() != null) geoPoint = new GeoPoint(d.getLatitude(), d.getLongitude());

            Document document = Document.newBuilder()
                                        .setId(d.getEventContentNo().toString())
                                        .addField(Field.newBuilder().setName("title").setText(d.getTitle()))
                                        .addField(Field.newBuilder().setName("eventDescText").setText(d.getEventDescText()))
                                        .addField(Field.newBuilder().setName("eventStart").setDate(d.getEventStart()))
                                        .addField(Field.newBuilder().setName("eventEnd").setDate(d.getEventEnd()))
                                        .addField(Field.newBuilder().setName("address").setText(d.getAddress()))
                                        .addField(Field.newBuilder().setName("tags").setText(d.getTags()))
                                        .addField(Field.newBuilder().setName("location").setGeoPoint(geoPoint))
                                        .addField(Field.newBuilder().setName("createNo").setText(String.valueOf(d.getCreateNo())))
                                        .build();


            return document;
        }).collect(Collectors.toList());
        AppengineUtils.indexADocuments(this.searchIndexName, documents);

        return documents.size();
    }

    private String queryGenerator(SearchIndexComb searchIndexComb ){
//        1. input 값은 공백이 있을 경우 OR로 판단하고 OR 조건을 넣어준다.
//        2. 같은 키워드로 검색하는 대상은 OR조건 , 다른 키워드를 대사으로 하는 조건은 AND로 한다.
//        http 제한문자임

//        String queryLongitude = searchIndexComb.getLongitude().toString();
        String queryString = "";


        if (searchIndexComb.getTitle() != null){

            queryString += String.format(" title = ( %s ) OR tags = ( %s ) OR eventDescText = ( %s ) OR address = ( %s ) "
                ,searchIndexComb.getTitle()
                ,searchIndexComb.getTitle()
                ,searchIndexComb.getTitle()
                ,searchIndexComb.getTitle());
            ;
        }
        if (searchIndexComb.getLatitude() != null && searchIndexComb.getLongitude() != null && searchIndexComb.getLocationDistance() != null ){

            queryString += String.format(" distance(location, geopoint(%f,%f)) < %d"
                ,searchIndexComb.getLatitude()
                ,searchIndexComb.getLongitude()
                ,searchIndexComb.getLocationDistance());
        }
        if (searchIndexComb.getEventStart() != null ){

            queryString +=  String.format(" eventStart <= %s eventEnd >= %s"
                ,DateUtils.addDayString(searchIndexComb.getEventStart(), "yyyy-MM-dd", 0)
                ,DateUtils.addDayString(searchIndexComb.getEventStart(),"yyyy-MM-dd",0));
            ;
        }



        log.debug("queryString:" + queryString);

        return queryString;
    }



    public Page<SearchIndexComb> searchTotalSearchIndex(SearchIndexComb searchKeyword, PageRequest pageRequest) throws InterruptedException {

        SortOptions sortOptions =
            SortOptions.newBuilder()
//                       .addSortExpression(
//                           SortExpression.newBuilder()
//                                         .setExpression("event_start")
//                                         .setDirection(SortExpression.SortDirection.ASCENDING)
//                                         .setDefaultValueNumeric(0)
//                       )
//                       .setLimit(20)
                       .build();


        QueryOptions options =
            QueryOptions.newBuilder()
                        .setLimit(pageRequest.getPageSize())       // fetch count
                        .setOffset(pageRequest.getPageNumber()*pageRequest.getPageSize())      // fetch start point
                        .setSortOptions(sortOptions)
                        .build();

        // [START search_document]
        final int maxRetry = 3;
        int attempts = 0;
        int delay = 2;
        while (true) {
            try {

                Query query = Query.newBuilder().setOptions(options)
                                   .build(queryGenerator(searchKeyword));

                Results<ScoredDocument> results = AppengineUtils.getIndexSpec(this.searchIndexName).search(query);
                return makeResultData(results,pageRequest);
            } catch (SearchException e) {
                if (StatusCode.TRANSIENT_ERROR.equals(e.getOperationResult().getCode())
                    && ++attempts < maxRetry) {
                    // retry
                    try {
                        Thread.sleep(delay * 500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                        // ignore
                    }
                    delay *= 2; // easy exponential backoff
                    continue;
                } else {
                    throw e;
                }
            }
        }
        // [END search_document]
        // We don't test the search result below, but we're fine if it runs without errors.
    }

    private Page<SearchIndexComb> makeResultData(Results<ScoredDocument> results,PageRequest defaultPageable) {

//        log.debug("getNumberFound:" + String.valueOf(results.getNumberFound()));
//        log.debug("getNumberReturned :"+String.valueOf(results.getNumberReturned()));



        List<SearchIndexComb> indexCombList = results.getResults().stream().map(d -> {
            return SearchIndexComb.builder()
                                  .eventContentNo(Integer.valueOf(d.getId()))
                                  .title(d.getOnlyField("title").getText())
                                  .createNo(Integer.valueOf(d.getOnlyField("createNo").getText()))
                                  .build();
        }).collect(Collectors.toList());

        int totalCount = (int) results.getNumberFound();
//        int returnCount = results.getNumberReturned();
        int start = (int)defaultPageable.getOffset();
//        int end = (start + defaultPageable.getPageSize()) > results.getNumberFound() ? (int) results.getNumberFound() : (start + defaultPageable.getPageSize());


        return  new PageImpl<SearchIndexComb>(indexCombList, defaultPageable, totalCount);
    }




}
