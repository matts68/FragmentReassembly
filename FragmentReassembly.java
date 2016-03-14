package fragment.submissions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/***
 * Enables to reassemble a given set of text fragments
 *
 * @author Matthieu S.
 *
 */
public class FragmentReassembly {

    /** Fragment separator character **/
    private static final String FRAGMENT_SEPARATOR = ";";

    /***
     * Manage fragments that have overlaps
     * @author Matthieu S.
     *
     */
    class OverlapFragmentAnalyzer {

        /***
         * Reassemble all the fragments into one
         * @param fragments fragments to process
         * @return remaining fragment that contains reassembled text
         */
        public String mergeFragments(final List<Fragment> fragments) {

            final List<Fragment> remainingFragmentsToProcess = new ArrayList<Fragment>(fragments);

            while (remainingFragmentsToProcess.size() > 1) {
                // Build combinaisons of fragment pairs
                final List<FragmentPair> fragmentPairs = buildFragmentPairs(remainingFragmentsToProcess);
                // Retrieve pair that has maximal overlap length
                int indexWithMaxOverlap = retrieveIndexOfFragmentPairWithMaximalOverlap(fragmentPairs);
                if (indexWithMaxOverlap != -1) {
                    // Merge fragments following kind of overlap
                    final FragmentPair fragmentPairToMerge = fragmentPairs.get(indexWithMaxOverlap);
                    fragmentPairToMerge.mergeFragments();
                    // Remove fragment that has be merged into the other one, and so becomes useless
                    remainingFragmentsToProcess.remove(fragmentPairToMerge.getUselessFragment());
                } else {
                    break;
                }
            }

            return concatenate(remainingFragmentsToProcess);

        }

        /***
         * Build all combinaisons possible of pairs of fragments
         * @param fragments pairs have to build from this list of fragments
         * @return pairs
         */
        private List<FragmentPair> buildFragmentPairs(final List<Fragment> fragments) {

            final List<FragmentPair> fragmentPairs = new ArrayList<FragmentPair>();

            for (int i = 0; i < fragments.size(); i++) {
                for (int j = 0; j < fragments.size(); j++) {
                    if (i != j) {
                        FragmentPair fragmentPair = new FragmentPair(fragments.get(i), fragments.get(j));
                        fragmentPairs.add(fragmentPair);
                    }
                }
            }

            return fragmentPairs;

        }

        /***
         * Retrieve fragment pair that contains maximal overlap value
         * @param fragmentPairs pairs that have to be analyzed
         * @return index of fragment pair, -1 if no fragment matches any more
         */
        private int retrieveIndexOfFragmentPairWithMaximalOverlap(final List<FragmentPair> fragmentPairs) {

            int indexWithMaxOverlap = -1;
            int currentMaxOverlap = 0;

            for (int i = 0; i < fragmentPairs.size(); i++) {
                int currentOverlap = fragmentPairs.get(i).giveMatchingLength();
                if (currentOverlap > currentMaxOverlap) {
                    currentMaxOverlap = currentOverlap;
                    indexWithMaxOverlap = i;
                }
            }

            return indexWithMaxOverlap;

        }

        /***
         * Concatenate remaining fragments by adding a separator between them
         * @param fragments fragments to display
         * @return string value that is the concatenation of fragments
         */
        private String concatenate(final List<Fragment> fragments) {

            StringBuilder fragmentsConcatenation = new StringBuilder();

            for (int i = 0; i < fragments.size(); i++) {
                if (!fragments.get(i).getLastValue().isEmpty()) {
                    if (!fragmentsConcatenation.toString().isEmpty()) {
                        fragmentsConcatenation.append(FRAGMENT_SEPARATOR);
                    }
                    fragmentsConcatenation.append(fragments.get(i).getLastValue());
                }
            }

            return fragmentsConcatenation.toString();

        }

    }

    class FragmentPair {

        private Fragment firstFragment;

        private Fragment secondFragment;

        private FragmentMatch fragmentMatch;

        public FragmentPair(final Fragment firstFragment, final Fragment secondFragment) {
            this.firstFragment = firstFragment;
            this.secondFragment = secondFragment;
        }

        public Fragment getUselessFragment() {

            // Both fragments match : eliminate the one that is useless
            if (this.fragmentMatch.bothFragmentsMatch()) {
                return this.firstFragment.getLastValue().isEmpty() ? this.firstFragment : this.secondFragment;
            }

            // Fragments do not match : no one is eliminated
            return null;
        }

        public Fragment getGreatestFragment() {

            return this.firstFragment.getLastValue().length() < this.secondFragment.getLastValue().length()
                ? this.secondFragment
                    : this.firstFragment;
        }

        public int giveMatchingLength() {
            this.fragmentMatch = new FragmentMatch(this.firstFragment, this.secondFragment);
            this.fragmentMatch.process();
            return this.fragmentMatch.getMatchingLength();
        }

        public void mergeFragments() {
            // Merge fragments is there is a matching string
            if (this.fragmentMatch.bothFragmentsMatch()) {
                if (MatchingType.START_OF_FIRST_FRAGMENT_MATCH_WITH_END_OF_SECOND_FRAGMENT.equals(this.fragmentMatch
                    .getMatchingType())) {
                    mergeFragmentsFromEndToStart(this.secondFragment, this.firstFragment,
                        this.fragmentMatch.getMatchingLength());
                } else if (MatchingType.START_OF_SECOND_FRAGMENT_MATCH_WITH_END_OF_FIRST_FRAGMENT
                    .equals(this.fragmentMatch.getMatchingType())) {
                    mergeFragmentsFromEndToStart(this.firstFragment, this.secondFragment,
                        this.fragmentMatch.getMatchingLength());
                } else {
                    // One fragment entirely contained : set it an empty value
                    if (this.fragmentMatch.matchingValue.equals(this.firstFragment.getLastValue())) {
                        this.firstFragment.setLastValue("");
                    } else {
                        this.secondFragment.setLastValue("");
                    }
                }
            }
        }

        private void mergeFragmentsFromEndToStart(final Fragment valueWhoseEndIsImpactedByMerged, final Fragment valueWhoseStartIsImpactedByMerged, final Integer mergeLength) {

            StringBuilder mergeValue = new StringBuilder();
            mergeValue.append(valueWhoseEndIsImpactedByMerged.getLastValue());
            if (valueWhoseStartIsImpactedByMerged.getLastValue().length() >= mergeLength) {
                mergeValue.append(valueWhoseStartIsImpactedByMerged.getLastValue().substring(mergeLength));
            }

            valueWhoseEndIsImpactedByMerged.setLastValue(mergeValue.toString());
            valueWhoseStartIsImpactedByMerged.setLastValue("");
        }

        @Override
        public String toString() {
            return "firstFragment = " + this.firstFragment + ", secondFragment = " + this.secondFragment;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof FragmentPair)) {
                return false;
            }
            FragmentPair fragmentPair = (FragmentPair) obj;
            return (this.firstFragment.equals(fragmentPair.firstFragment) && this.secondFragment
                .equals(fragmentPair.secondFragment)) || ((this.secondFragment.equals(fragmentPair.firstFragment) && this.firstFragment
                    .equals(fragmentPair.secondFragment)) || (this.firstFragment.equals(fragmentPair.secondFragment) && this.secondFragment
                .equals(fragmentPair.firstFragment)));
        }

    }

    class Fragment {

        private int position;

        private String originalValue;

        private String lastValue;

        public Fragment(final int position, final String originalValue) {
            this.position = position;
            this.originalValue = originalValue;
            this.lastValue = originalValue;
        }

        public int getPosition() {
            return this.position;
        }

        public String getOriginalValue() {
            return this.originalValue;
        }

        public void setLastValue(final String lastValue) {
            this.lastValue = lastValue;
        }

        public String getLastValue() {
            return this.lastValue;
        }

        @Override
        public String toString() {
            return this.lastValue;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof Fragment)) {
                return false;
            }
            return (this.position == ((Fragment) obj).position);
        }

    }

    class FragmentMatch {

        private Fragment firstFragment;

        private Fragment secondFragment;

        private String matchingValue;

        private int matchingLength;

        private MatchingType matchingType;

        public FragmentMatch(final Fragment firstFragment, final Fragment secondFragment) {
            this.firstFragment = firstFragment;
            this.secondFragment = secondFragment;
        }

        public boolean bothFragmentsMatch() {
            return this.matchingLength > 0;
        }

        public int getMatchingLength() {
            return this.matchingLength;
        }

        public MatchingType getMatchingType() {
            return this.matchingType;
        }

        public String getMatchingValue() {
            return this.matchingValue;
        }

        public void process() {

            // Check if start of one fragment matches with the end of the other one

            String matchEndToStartValue = giveMatchingEndToStartValue(this.firstFragment.getLastValue(),
                this.secondFragment.getLastValue());

            String reverseMatchEndToStartValue = giveMatchingEndToStartValue(this.secondFragment.getLastValue(),
                this.firstFragment.getLastValue());

            if ((matchEndToStartValue.length() > 0) && (matchEndToStartValue.length() >= reverseMatchEndToStartValue
                .length())) {
                this.matchingLength = matchEndToStartValue.length();
                this.matchingType = MatchingType.START_OF_SECOND_FRAGMENT_MATCH_WITH_END_OF_FIRST_FRAGMENT;
                this.matchingValue = matchEndToStartValue;
            } else if (reverseMatchEndToStartValue.length() > 0) {
                this.matchingLength = reverseMatchEndToStartValue.length();
                this.matchingType = MatchingType.START_OF_FIRST_FRAGMENT_MATCH_WITH_END_OF_SECOND_FRAGMENT;
                this.matchingValue = reverseMatchEndToStartValue;
            }

            // Check if one fragment is all included into the other one

            boolean oneFragmentWholeOverlapped = isOneStringIncludedIntoTheOtherOne(this.firstFragment.getLastValue(),
                this.secondFragment.getLastValue());

            if (oneFragmentWholeOverlapped) {
                String matchingValue = this.firstFragment.getLastValue().length() < this.secondFragment.getLastValue()
                    .length() ? this.firstFragment.getLastValue() : this.secondFragment.getLastValue();
                    if (matchingValue.length() > this.matchingLength) {
                        this.matchingLength = matchingValue.length();
                        this.matchingType = MatchingType.ONE_FRAGMENT_ENTIRELY_CONTAINED_INTO_THE_OTHER_ONE;
                        this.matchingValue = matchingValue;
                    }
            }

        }

        private String giveMatchingEndToStartValue(final String valueEndToAnalyze, final String valueStartToAnalyze) {

            StringBuilder currentValueToCompare = new StringBuilder();
            String matchingValue = "";

            if (valueEndToAnalyze.length() > 0) {
                for (int i = valueEndToAnalyze.length() - 1; i >= 0; i--) {
                    currentValueToCompare.insert(0, valueEndToAnalyze.charAt(i));
                    if ((valueStartToAnalyze.length() >= currentValueToCompare.toString().length()) && valueStartToAnalyze
                        .startsWith(currentValueToCompare.toString())) {
                        matchingValue = currentValueToCompare.toString();
                    }
                }
            }

            return matchingValue;

        }

        private boolean isOneStringIncludedIntoTheOtherOne(final String firstValue, final String secondValue) {

            return firstValue.contains(secondValue) || secondValue.contains(firstValue);

        }

    }

    enum MatchingType {
        START_OF_FIRST_FRAGMENT_MATCH_WITH_END_OF_SECOND_FRAGMENT,
        START_OF_SECOND_FRAGMENT_MATCH_WITH_END_OF_FIRST_FRAGMENT,
        ONE_FRAGMENT_ENTIRELY_CONTAINED_INTO_THE_OTHER_ONE;
    }

    public List<Fragment> buildFragments(final String value) {

        String[] fragmentValues = value.split(FRAGMENT_SEPARATOR);

        if (fragmentValues.length < 2) {
            throw new UnsupportedOperationException("There must be at least two fragments");
        }

        final List<Fragment> fragmentParts = new ArrayList<Fragment>();

        for (int i = 0; i < fragmentValues.length; i++) {
            final Fragment fragmentPart = new Fragment(i, fragmentValues[i]);
            fragmentParts.add(fragmentPart);
        }

        return fragmentParts;

    }

    /***
     * Reassemble text fragments by merging overlaps
     * @param textValue text fragments to reassemble
     * @return well-formed text
     */
    public String reassemble(final String textValue) {

        OverlapFragmentAnalyzer analyzer = new OverlapFragmentAnalyzer();
        List<Fragment> fragments = buildFragments(textValue);
        return analyzer.mergeFragments(fragments);

    }
	
	public static void main(final String[] args) throws FileNotFoundException, UnsupportedEncodingException {
	
        MatthieuSchittly textManager = new MatthieuSchittly();

        try (BufferedReader in = new BufferedReader(new FileReader(args[0]))) {            
            String fragmentProblem;
            while ((fragmentProblem = in.readLine()) != null) {
                System.out.println(textManager.reassemble(fragmentProblem));
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
