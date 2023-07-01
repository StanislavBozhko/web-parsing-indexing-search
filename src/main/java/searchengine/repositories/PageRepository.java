package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaDB;
import searchengine.model.PageDB;
import searchengine.model.SiteDB;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

@Repository
public interface PageRepository extends JpaRepository<PageDB, Integer> {

    //Iterable<PageDB>
    ArrayList<PageDB> findBySite(SiteDB siteDB);

    PageDB findById(int id);

    long countBySite(SiteDB site);
    Iterable<PageDB> findByPathAndSite(String path, SiteDB siteDB);

    @Transactional
    void deleteByPathAndSite(String path, SiteDB siteDB);

    @Query(value = "SELECT DISTINCT p.* FROM Page p JOIN `index` i ON p.id = i.page_id WHERE i.lemma_id IN :lemmas", nativeQuery = true)
    Set<PageDB> findByLemmaList(@Param("lemmas") Iterable<LemmaDB> lemmaList);

    @Query(value = "SELECT DISTINCT p.* FROM Page p JOIN `index` i ON p.id = i.page_id WHERE i.lemma_id = :lemma", nativeQuery = true)
    Set<PageDB> findByLemma(@Param("lemma") LemmaDB lemmaDB);

    @Query(value = "SELECT DISTINCT p.* FROM Page p JOIN `index` i ON p.id = i.page_id WHERE p.id IN :pages AND i.lemma_id = :lemma", nativeQuery = true)
    Set<PageDB> findByLemmaAndPageList(@Param("pages") Iterable<PageDB> pageDBSet,
                                       @Param("lemma") LemmaDB lemmaDB);
}
