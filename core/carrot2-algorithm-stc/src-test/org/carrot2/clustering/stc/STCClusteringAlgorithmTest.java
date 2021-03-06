
/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2013, Dawid Weiss, Stanisław Osiński.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package org.carrot2.clustering.stc;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.carrot2.core.Cluster;
import org.carrot2.core.ProcessingResult;
import org.carrot2.core.test.ClusteringAlgorithmTestBase;
import org.carrot2.core.test.SampleDocumentData;
import org.carrot2.text.preprocessing.CaseNormalizer;
import org.carrot2.util.attribute.AttributeUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

/**
 * Test cases for the {@link STCClusteringAlgorithm}.
 */
public class STCClusteringAlgorithmTest extends
    ClusteringAlgorithmTestBase<STCClusteringAlgorithm>
{
    @Override
    public Class<STCClusteringAlgorithm> getComponentClass()
    {
        return STCClusteringAlgorithm.class;
    }

    @Test
    public void testClusteringWithDfThreshold()
    {
        processingAttributes.put(
            AttributeUtils.getKey(CaseNormalizer.class, "dfThreshold"), 20);
        final Collection<Cluster> clustersWithThreshold = cluster(
            SampleDocumentData.DOCUMENTS_DATA_MINING).getClusters();

        // Clustering with df threshold must not fail
        assertThat(clustersWithThreshold.size()).isGreaterThan(0);
    }

    @Test
    public void testMaxClusters()
    {
        processingAttributes.put(
            AttributeUtils.getKey(STCClusteringAlgorithm.class, "maxClusters"), 9);
        
        final Collection<Cluster> clusters = 
            cluster(SampleDocumentData.DOCUMENTS_DATA_MINING).getClusters();

        // 9 + others
        assertThat(clusters.size()).isEqualTo(9 + 1);
    }

    @Test
    public void testComputeIntersection()
    {
        int [] t1;

        t1 = new int [] {0, 1, 2,   1, 2, 3};
        assertEquals(2, STCClusteringAlgorithm.computeIntersection(t1, 0, 3, t1, 3, 3));

        t1 = new int [] {0, 1, 2,   3, 5, 6};
        assertEquals(0, STCClusteringAlgorithm.computeIntersection(t1, 0, 3, t1, 3, 3));

        t1 = new int [] {0, 1, 2,   -1, 2, 6};
        assertEquals(1, STCClusteringAlgorithm.computeIntersection(t1, 0, 3, t1, 3, 3));

        t1 = new int [] {0, 1, 2,   0};
        assertEquals(1, STCClusteringAlgorithm.computeIntersection(t1, 0, 3, t1, 3, 1));
    }

    /**
     * CARROT-1008: STC is not using term stems.
     */
    @Test
    @Ignore
    public void testCarrot1008() throws Exception
    {
        ProcessingResult pr = ProcessingResult.deserialize(
            Resources.newInputStreamSupplier(
                Resources.getResource(this.getClass(), "CARROT-1008.xml")).getInput());

        STCClusteringAlgorithmDescriptor.attributeBuilder(processingAttributes)
            .maxClusters(30);

        pr = cluster(pr.getDocuments());

        Set<String> clusterLabels = collectClusterLabels(pr);
        assertThat(
            clusterLabels.contains("Guns") &&
            clusterLabels.contains("Gun")).isFalse();
    }

    private Set<String> collectClusterLabels(ProcessingResult pr)
    {
        final Set<String> clusterLabels = Sets.newHashSet();
        new Cloneable()
        {
            public void dumpClusters(List<Cluster> clusters, int depth) 
            {
                String indent = Strings.repeat("  ", depth);
                for (Cluster c : clusters) {
                    System.out.println(indent + c.getLabel());
                    clusterLabels.add(c.getLabel());
                    if (c.getSubclusters() != null) {
                        dumpClusters(c.getSubclusters(), depth + 1);
                    }
                }
            }
        }.dumpClusters(pr.getClusters(), 0);

        return clusterLabels;
    }
}
