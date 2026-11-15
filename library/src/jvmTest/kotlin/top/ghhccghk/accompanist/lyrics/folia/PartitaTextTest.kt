package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.test.*

class PartitaTextTest {
    @Test fun fittedFontKeepsSerializedClampPrecision() {
        // Actual pinned browser: clamp(1.917rem,4.217vw,3.450rem).
        val fit=4.217/5.5
        assertEquals(33.736,partitaFontSize(44.0,800.0,fit),.000001)
        assertEquals(40.0,partitaFontSize(40.0,380.0,1.0),.000001)
        assertEquals(72.0,partitaFontSize(72.0,1600.0,1.0),.000001)
    }
    @Test fun parserTimingCutsNeverBecomeAnInvalidGraphemeRowBreak() {
        val text="Á👩‍👩‍👧‍👦B"
        val source=SyncedLine(content=text,start=1000,end=5000,translation=null)
        val pieces=text.indices.map { TimedFragment(it,it+1,1_000_000+it*100_000L,1_000_000+(it+1)*100_000L) }
        val line=NormalizedLine(SourceRef(0),source,text,null,null,1_000_000,5_000_000,false,pieces)
        val layout=buildPartitaLayout(line,960.0,FoliaAnimationIntensity.Normal,PartitaConfig(semantic=false))
        var end=0
        val boundaries=graphemeBoundaries(text)
        layout.rows.forEach { row ->
            val value=row.words.joinToString(""){it.text}
            end+=value.length
            assertTrue(end in boundaries,"A row cannot split a surrogate, combining mark or ZWJ sequence")
            val shaped=shapeDisplayWords(row.words,value)
            assertEquals(value,shaped.joinToString(""){it.text})
            assertEquals(row.words.sumOf{it.fragments.size},shaped.sumOf{it.fragments.size})
            shaped.forEach { run ->
                val valid=graphemeBoundaries(run.text)
                run.graphemes.forEach {assertTrue(it.begin in valid && it.end in valid)}
            }
        }
        assertEquals(text.length,end)
        assertEquals(pieces.size,layout.rows.sumOf{r->r.words.sumOf{it.fragments.size}})
    }

    @Test fun lineOnlyDataRemainsOneExplicitlyLineTimedRun() {
        val text="one intact العربية line"
        val source=SyncedLine(content=text,start=1000,end=5000,translation=null)
        val line=NormalizedLine(SourceRef(0),source,text,null,null,1_000_000,5_000_000,false,emptyList())
        val row=buildPartitaLayout(line,600.0,FoliaAnimationIntensity.Normal).rows.single()
        assertEquals(text,row.words.single().text)
        assertEquals(1_000_000,row.words.single().startUs)
        assertEquals(5_000_000,row.words.single().endUs)
    }
}
