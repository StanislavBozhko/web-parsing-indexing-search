package searchengine.model;

import javax.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`index`", indexes = {@javax.persistence.Index(
        name = "page_id_list", columnList = "page_id"),
        @javax.persistence.Index(name = "lemma_id_list", columnList = "lemma_id")})
public class IndexDB implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", referencedColumnName = "id", nullable = false)
    private PageDB page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", referencedColumnName = "id", nullable = false)
    private LemmaDB lemma;

    @Column(name = "`rank`", nullable = false)
    private float rank;

    public IndexDB(PageDB page, LemmaDB lemmaDB, float rank) {
        this.page = page;
        this.lemma = lemmaDB;
        this.rank = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexDB indexDB = (IndexDB) o;
        return id == indexDB.id && Float.compare(indexDB.rank, rank) == 0 && Objects.equals(page, indexDB.page)
                && Objects.equals(lemma, indexDB.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, page, lemma, rank);
    }

    @Override
    public String toString() {
        return "IndexDB{" +
                "id=" + id +
                ", page=" + page +
                ", lemma=" + lemma +
                ", rank=" + rank +
                '}';
    }
}
