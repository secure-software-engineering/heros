/*******************************************************************************
 * Copyright (c) 2015 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.alias;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Sets;

import heros.alias.AccessPath.PrefixTestResult;
import heros.alias.SubAccessPath.*;

@SuppressWarnings("unchecked")
public class AccessPathTest {

	private static FieldRef f(String s) {
		return new FieldRef(s);
	}
	
	private static FieldRef[] f(String...s) {
		FieldRef[] result = new FieldRef[s.length];
		for(int i=0; i<s.length; i++) {
			result[i] = f(s[i]);
		}
		return result;
	}
	
	private static SetOfPossibleFieldAccesses<FieldRef> anyOf(String...fields) {
		Set<FieldRef> set = Sets.newHashSet();
		for(String f : fields)
			set.add(new FieldRef(f));
		return new SetOfPossibleFieldAccesses<FieldRef>(set);
	}
	
	private static SpecificFieldAccess<FieldRef> s(String field) {
		return new SpecificFieldAccess<FieldRef>(new FieldRef(field));
	}
	
	private static AccessPath<FieldRef> ap(SubAccessPath<FieldRef>... path) {
		return new AccessPath<FieldRef>(path, new Set[0]);
	}
	
	@Test
	public void addAndMergeAll() {
		AccessPath<FieldRef> sut = AccessPath.<FieldRef>empty().addFieldReference(f("a", "b", "c"));
		assertEquals(ap(anyOf("a","b","c")), sut.addFieldReference(f("a")));
	}

	@Test
	public void addAndMergeSuffix() {
		AccessPath<FieldRef> sut = AccessPath.<FieldRef>empty().addFieldReference(f("a", "b", "c"));
		assertEquals(ap(s("a"), anyOf("b","c")), sut.addFieldReference(f("b")));
	}
	
	@Test
	public void addWithoutMerge() {
		AccessPath<FieldRef> sut = ap(s("a"));
		assertEquals(ap(s("a"), s("b")), sut.addFieldReference(f("b")));
	}
	
	@Test
	public void addMergedFields() {
		AccessPath<FieldRef> sut = ap(s("a"));
		assertEquals(ap(anyOf("a")), sut.addFieldReference(anyOf("a")));
	}

	@Test
	public void addMergedFieldsOnExclusion() {
		AccessPath<FieldRef> sut = ap().appendExcludedFieldReference(f("a"));
		assertEquals(ap(anyOf("a", "b")), sut.addFieldReference(anyOf("a", "b")));
	}
	
	@Test
	public void addMergedFieldsOnNestedExclusion() {
		AccessPath<FieldRef> sut = ap().appendExcludedFieldReference(f("a")).appendExcludedFieldReference(f("b"));
		assertEquals(ap(anyOf("a", "b")), sut.addFieldReference(anyOf("a", "b")));
	}
	
	@Test
	public void addFieldThatMerges() {
		AccessPath<FieldRef> sut = ap(s("a"), s("b")).appendExcludedFieldReference(f("c")).appendExcludedFieldReference(f("x"));
		assertEquals(ap(anyOf("a", "b")).appendExcludedFieldReference(f("x")), sut.addFieldReference(s("a")));
	}
	
	@Test
	public void addFieldThatMergesResultingInExclusionOfMergedField() {
		AccessPath<FieldRef> sut = ap(s("a"), s("b")).appendExcludedFieldReference(f("c")).appendExcludedFieldReference(f("b"));
		assertEquals(ap(anyOf("a", "b")), sut.addFieldReference(s("a")));
	}
	
	@Test
	public void addFieldThatMergesResultingInExclusionOfMergedField2() {
		AccessPath<FieldRef> sut = ap().appendExcludedFieldReference(f("a")).appendExcludedFieldReference(f("b"));
		assertEquals(ap(anyOf("a", "c")), sut.addFieldReference(anyOf("a","c")));
	}
	
	@Test
	public void addFieldThatMergesResultingInExclusionOfMergedField3() {
		AccessPath<FieldRef> sut = ap(s("c")).appendExcludedFieldReference(f("a")).appendExcludedFieldReference(f("b"));
		assertEquals(ap(anyOf("c")), sut.addFieldReference(anyOf("c")));
	}
	
	@Test
	public void addOnExclusion() {
		AccessPath<FieldRef> sut = ap().appendExcludedFieldReference(f("a")).appendExcludedFieldReference(f("b"));
		assertEquals(ap(s("b")).appendExcludedFieldReference(f("b")), sut.addFieldReference(s("b")));
	}
	
	@Test
	public void addOnNestedExclusion() {
		AccessPath<FieldRef> sut = ap().appendExcludedFieldReference(f("a")).appendExcludedFieldReference(f("b"));
		assertEquals(ap(anyOf("a", "c")), sut.addFieldReference(anyOf("a", "c")));
	}

	@Test(expected=IllegalArgumentException.class)
	public void addMergedFieldsOnSingleExclusion() {
		AccessPath<FieldRef> sut = ap().appendExcludedFieldReference(f("a"));
		sut.addFieldReference(anyOf("a"));	
	}
	
	@Test
	public void prependWithoutMerge() {
		assertEquals(ap(s("c"), s("a"), s("b")), ap(s("a"), s("b")).prepend(f("c")));
	}
	
	@Test
	public void prependWithMerge() {
		assertEquals(ap(anyOf("a"), anyOf("b", "c")), ap(s("a"), anyOf("b", "c")).prepend(f("a")));
	}
	
	@Test
	public void prependAndMergeWithSet() {
		assertEquals(ap(anyOf("a", "b", "c")), ap(s("a"), anyOf("b", "c")).prepend(f("b")));
	}
	
	@Test
	public void remove() {
		assertEquals(ap(s("b")), ap(s("a"), s("b")).removeFirst(f("a")));
	}
	
	@Test
	public void dontRemoveMergedFields() {
		assertEquals(ap(anyOf("a", "b")), ap(anyOf("a", "b")).removeFirst(f("a")));
	}
	
	@Test
	public void removeMergedFieldsIfRemovingSuffix() {
		assertEquals(ap(), ap(anyOf("a", "b"), s("c")).removeFirst(f("c")));
	}
	
	@Test
	public void deltaDepth1() {
		SubAccessPath<FieldRef>[] actual = ap(s("a")).getDeltaTo(ap(s("a"), s("b")));
		assertArrayEquals(new SubAccessPath[] { s("b") }, actual);
	}
	
	@Test
	public void deltaDepth2() {
		SubAccessPath<FieldRef>[] actual = ap(s("a")).getDeltaTo(ap(s("a"), s("b"), s("c")));
		assertArrayEquals(new SubAccessPath[] { s("b"), s("c") }, actual);
	}
	
	@Test
	public void delta() {
		SubAccessPath<FieldRef>[] actual = ap(s("a")).getDeltaTo(ap(s("a"), anyOf("b")));
		assertArrayEquals(new SubAccessPath[] { anyOf("b") }, actual);
	}
	
	@Test
	public void delta2() {
		SubAccessPath<FieldRef>[] actual = ap(s("f"), s("g"), s("h")).getDeltaTo(ap(anyOf("f", "g"), s("h")));
		assertArrayEquals(new SubAccessPath[] {  }, actual);
	}
	
	@Test
	public void delta3() {
		SubAccessPath<FieldRef>[] actual = ap(s("f"), s("f")).getDeltaTo(ap(anyOf("f")));
		assertArrayEquals(new SubAccessPath[] { anyOf("f") } , actual);
	}
	
	@Test
	public void deltaMatchingMergedField() {
		SubAccessPath<FieldRef>[] actual = ap(s("a"), s("b")).getDeltaTo(ap(s("a"), anyOf("b")));
		assertArrayEquals(new SubAccessPath[] { anyOf("b") }, actual);
	}
	
	@Test
	public void prefixOfMergedField() {
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, ap(s("f")).isPrefixOf(ap(anyOf("f"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, ap(s("f")).isPrefixOf(ap(anyOf("f", "g"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, ap(s("f")).isPrefixOf(ap(anyOf("f"), s("h"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, ap(s("f"), s("h")).isPrefixOf(ap(anyOf("f"), s("h"))));
	}
	
	@Test
	public void noPrefixOfMergedField() {
		assertEquals(PrefixTestResult.NO_PREFIX, ap(s("g")).isPrefixOf(ap(anyOf("f"))));
		assertEquals(PrefixTestResult.NO_PREFIX, ap(s("g")).isPrefixOf(ap(anyOf("f"), s("h"))));
	}
	
	@Test
	public void prefixOfExclusion() {
		AccessPath<FieldRef> sut = ap().appendExcludedFieldReference(f("f"));
		assertEquals(PrefixTestResult.NO_PREFIX, sut.isPrefixOf(ap(anyOf("f"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, sut.isPrefixOf(ap(anyOf("f", "g"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, sut.isPrefixOf(ap(anyOf("g"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, sut.isPrefixOf(ap(anyOf("f"), s("h"))));
	}
	
	@Test
	public void prefixOfExclusions() {
		AccessPath<FieldRef> sut = ap().appendExcludedFieldReference(f("f", "g"));
		assertEquals(PrefixTestResult.NO_PREFIX, sut.isPrefixOf(ap(anyOf("f"))));
		assertEquals(PrefixTestResult.NO_PREFIX, sut.isPrefixOf(ap(anyOf("f", "g"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX, sut.isPrefixOf(ap(anyOf("f"), s("h"))));
	}
	
	@Test
	public void mergedFieldsPrefixOf() {
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f")).isPrefixOf(ap(s("f"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f")).isPrefixOf(ap(s("g"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f")).isPrefixOf(ap().appendExcludedFieldReference(f("f"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f")).isPrefixOf(ap().appendExcludedFieldReference(f("f", "g"))));
		
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f"), s("h")).isPrefixOf(ap(s("f"), s("h"))));
		assertEquals(PrefixTestResult.GUARANTEED_PREFIX,  ap(anyOf("f")).appendExcludedFieldReference(f("f")).isPrefixOf(ap(s("f"), s("h"))));
	}
}
