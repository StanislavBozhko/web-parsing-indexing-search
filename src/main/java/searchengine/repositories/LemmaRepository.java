package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaDB;
import searchengine.model.SiteDB;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaDB, Integer> {

    long countBySiteEntityId(SiteDB site);

    @Query(value = "SELECT * FROM Lemma WHERE lemma IN :lemmas AND site_id = :site", nativeQuery = true)
    List<LemmaDB> findLemmaListBySite(@Param("lemmas") Iterable<String> lemmaList,
                                    @Param("site") SiteDB site);

    @Query(value = "SELECT l.* FROM Lemma l WHERE l.lemma = :lemma ORDER BY frequency ASC", nativeQuery = true)
    List<LemmaDB> findByLemma(@Param("lemma") String lemma);
}
