package somun.appengine.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.GeoPoint;
import com.google.appengine.api.search.GetRequest;
import com.google.appengine.api.search.GetResponse;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.Query;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchException;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.appengine.api.search.SortOptions;
import com.google.appengine.api.search.StatusCode;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import lombok.extern.slf4j.Slf4j;
import somun.appengine.AppengineUtils;
import somun.appengine.service.repository.SearchIndexComb;

@Slf4j
@SpringBootTest
public class CreateContentIndexTest {

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper();
    String INDEX = "contentIndex_1";
    List<JSONObject> indexSeenData;

    @Before
    public void setUp() throws Exception {
        helper.setUp();

        //  순번 , 위치(long,lati)
        List<SearchIndexComb> docList = new ArrayList<>();


        Stream.iterate(0,d->{
            docList.addAll(Arrays.asList(
                SearchIndexComb.builder().eventContentNo( 1+d).eventDescText("Cum cedrium peregrinatione, omnes medicinaes desiderium rusticus, flavum parses.").tags("Aa BB CC").longitude(127.03068730).latitude(37.50258907).build()
                ,
                SearchIndexComb.builder().eventContentNo( 2+d).eventDescText("Salvus, talis gabaliums absolute transferre de gratis, grandis urbs.").tags("Dd EE FF").longitude(126.95271200).latitude(37.48121000).build()
                ,
                SearchIndexComb.builder().eventContentNo( 3+d).eventDescText("Cum tabes ire, omnes gabaliumes resuscitabo velox, ferox orgiaes.").tags("Gg HH II").longitude(127.07473757).latitude(37.55555391).build()
            ));
            return d+3;
        }).limit(60).count();


        indexSeenData = docList.stream().map(d -> {
                            JSONObject doc = new JSONObject();
                            try {
                                doc.put("eventContentNo", d.getEventContentNo().toString());
                                doc.put("desc", d.getEventDescText());
                                doc.put("tags", d.getTags());
                                doc.put("event_start", new Date((new Date()).getTime() - (1000 * 60 * 60 * 24 * d.getEventContentNo())));
                                doc.put("event_end", new Date((new Date()).getTime() +  (1000 * 60 * 60 * 24 * d.getEventContentNo())));
                                doc.put("longitude", d.getLongitude());
                                doc.put("latitude",d.getLatitude());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return doc;
                        }).collect(Collectors.toList());

        createDocument();
    }

    @After
    public void tearDown() throws Exception {
        helper.tearDown();
    }


    private void createDocument() throws InterruptedException {
        List<Document> documents = indexSeenData.stream().map((d) -> {
            Document document = null;
            try {
                GeoPoint geoPoint = new GeoPoint(d.getDouble("latitude"), d.getDouble("longitude"));
                document = Document.newBuilder()
                                   .setId(d.getString("eventContentNo"))
                                   .addField(Field.newBuilder().setName("desc").setText(d.getString("desc")))
                                   .addField(Field.newBuilder().setName("tags").setText(d.getString("tags")))
                                   .addField(Field.newBuilder().setName("event_start").setDate((Date)d.get("event_start")))
                                   .addField(Field.newBuilder().setName("event_end").setDate((Date)d.get("event_end")))
                                   .addField(Field.newBuilder().setName("location").setGeoPoint(geoPoint))
                                   .build();
                log.debug(document.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return document;
        }).collect(Collectors.toList());
        AppengineUtils.indexADocuments(INDEX, (Iterable<Document>) documents);


        log.debug("##### Indexed a new document.");
        // [START get_document]
        IndexSpec indexSpec = IndexSpec.newBuilder().setName(INDEX).build();
        Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);


        // Fetch a single document by its  doc_id
        Document doc = index.get("1");
        // Fetch a range of documents by their doc_ids
//        log.debug ("myField: " + doc.getOnlyField("desc").getText());

        GetResponse<Document> docs =
        index.getRange(GetRequest.newBuilder().setStartId("1").setLimit(100).build());
    // [END get_document]

//        log.debug ("myField: " + docs.getResults().get(2).getOnlyField("desc").getText());

    }


    @Test
    public void updateDocument() throws InterruptedException {

        Document document = Document.newBuilder()
                                 .setId("1")
                                 .addField(Field.newBuilder().setName("desc").setText("update"))
                                 .build();

        IndexSpec indexSpec = IndexSpec.newBuilder().setName(INDEX).build();
        Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

        AppengineUtils.indexADocument(INDEX,document);

        // Fetch a single document by its  doc_id
        Document doc = index.get("1");
        // Fetch a range of documents by their doc_ids
        log.debug ("myField: " + doc.getOnlyField("desc").getText());
    }

    @Test
    public void searchDocument() throws InterruptedException {

        String queryString = ""
            + " desc = (tabes OR omnes )"     // or 조건일때 (공백을 넣고 검색을 하면 기본으로 OR 조건으로 인식하자)
//            + " OR tags = Gg"     // or 조건일때 (공백을 넣고 검색을 하면 기본으로 OR 조건으로 인식하자)
//            + " desc = (tabes  omnes )"     // and 조건일때
//            + " event_start <= 2018-06-24  event_end >= 2018-06-24"
//            + " distance(location, geopoint(37.50258907,127.03068730)) < 1000"
            ;

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
                        .setLimit(10)       // fetch count
                        .setOffset(10)      // fetch start point
                        .setSortOptions(sortOptions)
                        .build();


        // [START search_document]
        final int maxRetry = 3;
        int attempts = 0;
        int delay = 2;
        while (true) {
            try {

                Query query = Query.newBuilder().setOptions(options).build(queryString);

                Results<ScoredDocument> results = AppengineUtils.getIndexSpec(INDEX).search(query);

                log.debug("getNumberFound:" + String.valueOf(results.getNumberFound()));
                log.debug("getNumberReturned :"+String.valueOf(results.getNumberReturned()));

                log.debug(results.toString());

                // Iterate over the documents in the results
                for (ScoredDocument document : results) {
                    // handle results
                    log.debug(document.toString());
                }
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
            break;
        }
        // [END search_document]
        // We don't test the search result below, but we're fine if it runs without errors.
    }

}
