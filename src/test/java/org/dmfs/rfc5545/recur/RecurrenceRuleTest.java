/*
 * Copyright 2018 Marten Gajda <marten@dmfs.org>
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dmfs.rfc5545.recur;

import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Weekday;
import org.junit.jupiter.api.Test;

import static org.dmfs.jems2.hamcrest.matchers.LambdaMatcher.having;
import static org.dmfs.jems2.hamcrest.matchers.fragile.BrokenFragileMatcher.throwing;
import static org.dmfs.jems2.hamcrest.matchers.single.SingleMatcher.hasValue;
import static org.dmfs.rfc5545.Weekday.*;
import static org.dmfs.rfc5545.hamcrest.GeneratorMatcher.generates;
import static org.dmfs.rfc5545.hamcrest.RecurrenceRuleMatcher.*;
import static org.dmfs.rfc5545.hamcrest.datetime.BeforeMatcher.before;
import static org.dmfs.rfc5545.hamcrest.datetime.DayOfMonthMatcher.onDayOfMonth;
import static org.dmfs.rfc5545.hamcrest.datetime.MonthMatcher.inMonth;
import static org.dmfs.rfc5545.hamcrest.datetime.WeekDayMatcher.onWeekDay;
import static org.dmfs.rfc5545.hamcrest.datetime.YearMatcher.inYear;
import static org.dmfs.rfc5545.recur.RecurrenceRule.RfcMode.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 * @author Marten Gajda
 */
public final class RecurrenceRuleTest
{
    @Test
    public void test() throws InvalidRecurrenceRuleException
    {
        assertThat(new RecurrenceRule("FREQ=WEEKLY;COUNT=1000"),
            is(validRule(DateTime.parse("20180101"),
                walking(),
                instances(are(onWeekDay(MO))),
                results(1000))));

        assertThat(new RecurrenceRule("FREQ=MONTHLY;INTERVAL=1;BYDAY=+3TH;UNTIL=20140101T045959Z;WKST=SU"),
            is(validRule(DateTime.parse("20130101T050000Z"),
                walking(),
                instances(are(onWeekDay(TH), onDayOfMonth(15, 16, 17, 18, 19, 20, 21), inYear(2013), before("20140101T050000Z"))),
                startingWith("20130117T050000Z", "20130221T050000Z", "20130321T050000Z"),
                results(12))));

        // see https://github.com/dmfs/lib-recur/issues/73
        assertThat(new RecurrenceRule("FREQ=WEEKLY;INTERVAL=2;WKST=SU;BYDAY=TU;UNTIL=20200430T170000Z"),
            is(validRule(DateTime.parse("20200404T100000Z"),
                walking(),
                instances(are(onWeekDay(TU), onDayOfMonth(14, 28), inMonth(4), inYear(2020), before("20200430T170000Z"))),
                startingWith("20200414T100000Z", "20200428T100000Z"),
                results(2))));

        // see https://github.com/dmfs/lib-recur/issues/78
        assertThat(
            () -> {
                RecurrenceRule recurrenceRule = new RecurrenceRule(Freq.MONTHLY);
                recurrenceRule.setCount(5);
                recurrenceRule.setInterval(1);
                recurrenceRule.setSkip(RecurrenceRule.Skip.FORWARD);
                recurrenceRule.setWeekStart(Weekday.MO);
                return recurrenceRule;
            },
            hasValue(hasToString("FREQ=MONTHLY;RSCALE=GREGORIAN;SKIP=FORWARD;COUNT=5"))
        );

        assertThat(new RecurrenceRule("FREQ=MONTHLY;BYDAY=1MO,-1MO,WE"),
            is(validRule(DateTime.parse("20200902"),
                walking(),
                instances(are(onWeekDay(MO, WE))),
                startingWith("20200902", "20200907", "20200909", "20200916", "20200923", "20200928", "20200930", "20201005", "20201007"))));

        assertThat(new RecurrenceRule("FREQ=WEEKLY;INTERVAL=2;BYHOUR=13;BYDAY=MO;COUNT=2"),
            is(validRule(DateTime.parse("20201230T000000"),
                walking(),
                instances(are(onWeekDay(MO))),
                startingWith("20210111T130000", "20210125T130000"))));

        String ruleToTest = "FREQ=WEEKLY;BYMONTH=11;COUNT=1";
        RecurrenceRule rule = new RecurrenceRule(ruleToTest);
        RecurrenceRuleIterator iterator = rule.iterator(DateTime.parse("20210701T120000Z"));
        System.out.println(rule.getByPart(RecurrenceRule.Part.BYMONTH));
        System.out.println(rule.toString());
    }


    /**
     * see https://github.com/dmfs/lib-recur/issues/109
     */
    @Test
    void testAllDayUntilAndDateTimeStart() throws InvalidRecurrenceRuleException
    {
        assertThat(new RecurrenceRule("FREQ=DAILY;BYHOUR=12;UNTIL=20230305", RFC5545_LAX),
            allOf(validRule(DateTime.parse("20230301T000000"),
                    walking(),
                    results(5)),
                generates("20230301T000000",
                    "20230301T120000",
                    "20230302T120000",
                    "20230303T120000",
                    "20230304T120000",
                    "20230305T120000")));

        assertThat(new RecurrenceRule("FREQ=DAILY;BYHOUR=12;UNTIL=20230305", RFC2445_LAX),
            allOf(validRule(DateTime.parse("20230301T000000"),
                    walking(),
                    results(5)),
                generates("20230301T000000",
                    "20230301T120000",
                    "20230302T120000",
                    "20230303T120000",
                    "20230304T120000",
                    "20230305T120000")));

        assertThat(new RecurrenceRule("FREQ=DAILY;UNTIL=20230305", RFC5545_LAX),
            allOf(validRule(DateTime.parse("20230301T000000"),
                    walking(),
                    results(5)),
                generates("20230301T000000",
                    "20230301T000000",
                    "20230302T000000",
                    "20230303T000000",
                    "20230304T000000",
                    "20230305T000000")));

        assertThat(new RecurrenceRule("FREQ=DAILY;UNTIL=20230305", RFC2445_LAX),
            allOf(validRule(DateTime.parse("20230301T000000"),
                    walking(),
                    results(5)),
                generates("20230301T000000",
                    "20230301T000000",
                    "20230302T000000",
                    "20230303T000000",
                    "20230304T000000",
                    "20230305T000000")));

        assertThat(new RecurrenceRule("FREQ=DAILY;BYHOUR=12;UNTIL=20230305", RFC5545_STRICT),
            is(having(
                r -> () -> r.iterator(DateTime.parse("20230301T000000")), is(throwing(IllegalArgumentException.class)))));

        assertThat(new RecurrenceRule("FREQ=DAILY;BYHOUR=12;UNTIL=20230305", RFC2445_STRICT),
            is(having(
                r -> () -> r.iterator(DateTime.parse("20230301T000000")), is(throwing(IllegalArgumentException.class)))));
    }
}
