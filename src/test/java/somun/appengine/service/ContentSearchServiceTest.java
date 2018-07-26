package somun.appengine.service;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.GeoPoint;
import com.google.appengine.api.search.GetRequest;
import com.google.appengine.api.search.GetResponse;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.appengine.tools.development.testing.LocalSearchServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import lombok.extern.slf4j.Slf4j;
import somun.appengine.AppengineApplication;
import somun.appengine.AppengineUtils;
import somun.appengine.config.properties.SomunProperties;
import somun.appengine.service.repository.SearchIndexComb;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AppengineApplication.class)
public class ContentSearchServiceTest {


    private final LocalServiceTestHelper helper = new LocalServiceTestHelper();

    @Autowired
    SomunProperties somunProperties;

    @Autowired
    ContentSearchService contentSearchService;

    RestTemplate restTemplate;
    String indexName;

    @Before
    public void setUp() throws Exception {
        helper.setUp();
        restTemplate = new RestTemplate();
        indexName = "ContentSearchServiceTest_contentIndex_1";
//      index는 최초 1회 실행시만 하면 됨
//      contentSearchService.mergeTotalSearchIndex(somunProperties.getApiServer() + "/Content/V1/indexDocList/2018-01-01/2019-01-01?page=0&size=100");
    }

    @After
    public void tearDown() throws Exception {
        helper.tearDown();
//        LocalSearchServiceTestConfig.getLocalSearchService().stop();
//        FileUtils.deleteDirectory(new File("WEB-INF"));

    }

    @Test
    public  void mergeTotalSearchIndex() throws InterruptedException {
        int searchIndex = contentSearchService.mergeTotalSearchIndex(somunProperties.getApiServer() +
                                                               "/Content/V1/indexDocList/2018-01-01/2019-01-01?page=0&size=1000000");
    }
    @Test
    public void search_input_Date_location() throws InterruptedException {
        Page<SearchIndexComb> searchIndexCombs =
            contentSearchService.searchTotalSearchIndex(SearchIndexComb.builder()
                                                                       .title("2017")
//                                                                        .eventStart(new Date())
//                                                                        .latitude((double)37.778268)
//                                                                        .longitude((double) 128.876615)
//                                                                        .locationDistance(1000)
                                                                        .build()
                , new PageRequest(0, 10));

        assertThat(searchIndexCombs.getContent().size()).isGreaterThan(0);
    }


    @Test
    public void search_input() throws InterruptedException {

        Page<SearchIndexComb> searchIndexCombs =
            contentSearchService.searchTotalSearchIndex(SearchIndexComb.builder().title("2018").build()
            , new PageRequest(0, 10));


        assertThat(searchIndexCombs.getContent().size()).isGreaterThan(0);

        //PageRequest pageable = new PageRequest(pageableParam.getPageNumber(), pageableParam.getPageSize(), new Sort(Sort.Direction.DESC, "contentActivityNo")); //현재페이지, 조회할 페이지수, 정렬정보

    }


    @Test
    public void search_paging_test() throws InterruptedException {
        PageRequest pageRequest = new PageRequest(0, 10);//현재페이지, 조회할 row수
        Page<SearchIndexComb> searchIndexCombs = contentSearchService.searchTotalSearchIndex(SearchIndexComb.builder().title("2018").build(), pageRequest);

        while (!searchIndexCombs.isLast()){

            pageRequest = new PageRequest(searchIndexCombs.getNumber()+1, 10);
            searchIndexCombs = contentSearchService.searchTotalSearchIndex(SearchIndexComb.builder().title("2018").build(), pageRequest);


//            searchIndexCombs.getContent().stream().map(d->{log.debug(d.toString()); return 0;}).count();
//            log.debug("page 2 getTotalElements:"+ searchIndexCombs.getTotalElements());
//            log.debug("page 2 getTotalPages:"+ searchIndexCombs.getTotalPages());
//            log.debug("page 2 getNumber:"+ searchIndexCombs.getNumber());
//            log.debug("page 2 getNumberOfElements:"+ searchIndexCombs.getNumberOfElements());
//            log.debug("page 2 getSize:"+ searchIndexCombs.getSize());
//            log.debug("page 2 isLast:"+ searchIndexCombs.isLast());


        }

        assertThat(searchIndexCombs).isNotNull();

    }

    @Test
    public void search_startDate() throws InterruptedException {
        Page<SearchIndexComb> searchIndexCombs = contentSearchService.searchTotalSearchIndex(SearchIndexComb.builder()
                                                                                                            .eventStart(new Date())
                                                                                                            .build(),
                                                                                             new PageRequest(0, 10));
        assertThat(searchIndexCombs.getContent().size()).isGreaterThan(0);


    }

    @Test
    public void search_location() throws InterruptedException {
        Page<SearchIndexComb> searchIndexCombs = contentSearchService.searchTotalSearchIndex(SearchIndexComb.builder()
                                                                                                            .latitude((double) 35.238584)
                                                                                                            .longitude((double) 128.656963)
                                                                                                            .locationDistance(1000)
                                                                                                            .build(),
                                                                                             new PageRequest(0, 10));
        assertThat(searchIndexCombs.getContent().size()).isGreaterThan(0);
    }



    @Ignore
    public void pageSample() {
     /*
       public class SearchIndexCombPage extends PageImpl<SearchIndexComb> {

	@JsonCreator
	// Note: I don't need a sort, so I'm not including one here.
	// It shouldn't be too hard to add it in tho.
	public SearchIndexCombPage(@JsonProperty("content") List<SearchIndexComb> content,
							   @JsonProperty("number") int number,
							   @JsonProperty("size") int size,
							   @JsonProperty("totalElements") Long totalElements) {
		super(content, new PageRequest(number, size), totalElements);
	}

}
Page<SearchIndexComb> searchIndexComb;
        ResponseEntity<SearchIndexCombPage> forEntity = restTemplate.getForEntity(
            somunProperties.getApiServerUrl() + "/Content/V1/indexDocList/2018-01-01/2019-01-01?page=0&size=3"
            , SearchIndexCombPage.class);

        log.debug(forEntity.toString());*/
    }

    @Ignore
    public void mergeIntoIndex() throws InterruptedException {



        PagedResources<SearchIndexComb> response =
                                                restTemplate.exchange(somunProperties.getApiServer() + "/Content/V1/indexDocList/2018-01-01/2019-01-01?page=0&size=100"
                                                    ,HttpMethod.GET
                                                    , null
                                                    , new ParameterizedTypeReference<PagedResources<SearchIndexComb>>() {})
                                                .getBody();

        PagedResources.PageMetadata pageInfo = response.getMetadata();
        Collection<SearchIndexComb> searchIndexContents = response.getContent();

        List<Document> documents = searchIndexContents.stream().map((d) -> {
            GeoPoint geoPoint = new GeoPoint(d.getLatitude(), d.getLongitude());
            Document document = Document.newBuilder()
                                        .setId(d.getEventContentNo().toString())
                                        .addField(Field.newBuilder().setName("title").setText(d.getTitle()))
                                        .addField(Field.newBuilder().setName("eventDescText").setText(d.getEventDescText()))
                                        .addField(Field.newBuilder().setName("eventStart").setDate(d.getEventStart()))
                                        .addField(Field.newBuilder().setName("eventEnd").setDate(d.getEventEnd()))
                                        .addField(Field.newBuilder().setName("tags").setText(d.getTags()))
                                        .addField(Field.newBuilder().setName("location").setGeoPoint(geoPoint))
                                        .build();
            return document;
        }).collect(Collectors.toList());

        AppengineUtils.indexADocuments(this.indexName,documents);
        Document document =
            Document.newBuilder()
                    .setId("id_1")
                    .addField(Field.newBuilder().setName("desc").setText("�󼼼��� �󼼼���"))
                    .build();
        try {
            AppengineUtils.indexADocument(this.indexName, document);
        } catch (InterruptedException e) {

//            return "Interrupted";
        }
        log.info("##### Indexed a new document.");
        // [START get_document]
        IndexSpec indexSpec = IndexSpec.newBuilder().setName(this.indexName).build();
        Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

        // Fetch a single document by its  doc_id
        Document doc = index.get("id_1");

        // Fetch a range of documents by their doc_ids
        GetResponse<Document> docs =
            index.getRange(GetRequest.newBuilder().setStartId("id_1").setLimit(100).build());
        // [END get_document]
//        return ("myField: " + docs.getResults().get(0).getOnlyField("desc").getText());

    }

}
