package searchengine.model;

import javax.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lemma", indexes = {@javax.persistence.Index(name = "lemma_list", columnList = "lemma")})
public class LemmaDB implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(targetEntity = SiteDB.class)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SiteDB siteEntityId;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<IndexDB> index = new ArrayList<>();

    public LemmaDB(SiteDB siteEntityId, String lemma, int frequency) {
        this.siteEntityId = siteEntityId;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LemmaDB lemmaDB = (LemmaDB) o;
        return id == lemmaDB.id && frequency == lemmaDB.frequency
                && siteEntityId.equals(lemmaDB.siteEntityId) && lemma.equals(lemmaDB.lemma)
                ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, siteEntityId, lemma, frequency);
    }

    @Override
    public String toString() {
        return "LemmaDB{" +
                "id=" + id +
                ", site=" + siteEntityId +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                '}';
    }
}
