import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { FormRendererService } from './form-renderer.service';
import { AppraisalTemplateSchema } from './form-renderer.models';

const MOCK_SCHEMA: AppraisalTemplateSchema = {
  version: '3.0',
  sections: [
    {
      sectionType: 'header',
      title: 'Employee Information',
      items: []
    },
    {
      sectionType: 'rating_key',
      title: 'Rating Key',
      items: []
    },
    {
      sectionType: 'key_responsibilities',
      title: 'Key Responsibilities',
      items: [
        { id: 'kr_1', label: 'Essential Duty 1', ratingScale: 'competency' },
        { id: 'kr_2', label: 'Essential Duty 2', ratingScale: 'competency' }
      ]
    },
    {
      sectionType: 'idp',
      title: 'Individual Development Plan',
      items: [
        { id: 'idp_nextgen', label: 'NextGen Tech Skills', ratingScale: 'competency' },
        { id: 'idp_value', label: 'Value Addition', ratingScale: 'competency' },
        { id: 'idp_leadership', label: 'Leadership', ratingScale: 'competency' }
      ]
    },
    {
      sectionType: 'policy_adherence',
      title: 'Company Policies',
      items: [
        { id: 'policy_hr', label: 'Follow HR Policy', ratingScale: 'policy_1_10' }
      ]
    },
    {
      sectionType: 'goals',
      title: 'Goals',
      items: [
        { id: 'goal_1', label: 'Goal 1', ratingScale: 'competency' }
      ]
    }
  ]
};

describe('FormRendererService', () => {
  let service: FormRendererService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection()]
    });
    service = TestBed.inject(FormRendererService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('parseTemplateSchema', () => {
    it('should parse valid schema JSON', () => {
      const json = JSON.stringify(MOCK_SCHEMA);
      const result = service.parseTemplateSchema(json);
      expect(result.version).toBe('3.0');
      expect(result.sections.length).toBe(6);
    });

    it('should throw on invalid JSON', () => {
      expect(() => service.parseTemplateSchema('not-json')).toThrow();
    });

    it('should throw when sections are missing', () => {
      expect(() => service.parseTemplateSchema('{"version":"3.0"}')).toThrow();
    });
  });

  describe('renderSections', () => {
    it('should exclude rating_key from rendered sections', () => {
      const rendered = service.renderSections(MOCK_SCHEMA);
      const types = rendered.map(s => s.sectionType);
      expect(types).not.toContain('rating_key');
    });

    it('should render items with correct data keys', () => {
      const rendered = service.renderSections(MOCK_SCHEMA);
      const krSection = rendered.find(s => s.sectionType === 'key_responsibilities')!;
      expect(krSection.items[0].dataKey).toBe('keyResponsibilities[0]');
      expect(krSection.items[1].dataKey).toBe('keyResponsibilities[1]');
    });

    it('should render idp items with correct data keys', () => {
      const rendered = service.renderSections(MOCK_SCHEMA);
      const idpSection = rendered.find(s => s.sectionType === 'idp')!;
      expect(idpSection.items[0].dataKey).toBe('idp[0]');
    });
  });

  describe('getFieldEditability', () => {
    it('should allow employee to edit self fields when NOT_STARTED', () => {
      const result = service.getFieldEditability(true, false, 'NOT_STARTED');
      expect(result.selfCommentEditable).toBe(true);
      expect(result.selfRatingEditable).toBe(true);
      expect(result.managerCommentEditable).toBe(false);
    });

    it('should allow employee to edit self fields when DRAFT_SAVED', () => {
      const result = service.getFieldEditability(true, false, 'DRAFT_SAVED');
      expect(result.selfCommentEditable).toBe(true);
    });

    it('should prevent employee from editing after SUBMITTED', () => {
      const result = service.getFieldEditability(true, false, 'SUBMITTED');
      expect(result.selfCommentEditable).toBe(false);
      expect(result.selfRatingEditable).toBe(false);
    });

    it('should allow manager to edit manager fields when UNDER_REVIEW', () => {
      const result = service.getFieldEditability(false, true, 'UNDER_REVIEW');
      expect(result.managerCommentEditable).toBe(true);
      expect(result.managerRatingEditable).toBe(true);
      expect(result.selfCommentEditable).toBe(false);
    });

    it('should prevent all edits when REVIEWED_AND_COMPLETED', () => {
      const result = service.getFieldEditability(false, true, 'REVIEWED_AND_COMPLETED');
      expect(result.managerCommentEditable).toBe(false);
      expect(result.managerRatingEditable).toBe(false);
    });
  });

  describe('validateTemplateCompleteness', () => {
    it('should pass for a complete template', () => {
      const result = service.validateTemplateCompleteness(MOCK_SCHEMA);
      expect(result.valid).toBe(true);
      expect(result.missing.length).toBe(0);
    });

    it('should report missing sections', () => {
      const incomplete: AppraisalTemplateSchema = {
        version: '3.0',
        sections: [{ sectionType: 'header', title: 'Header', items: [] }]
      };
      const result = service.validateTemplateCompleteness(incomplete);
      expect(result.valid).toBe(false);
      expect(result.missing).toContain('key_responsibilities');
    });
  });

  describe('getRatingOptions', () => {
    it('should return competency labels for competency scale', () => {
      const options = service.getRatingOptions('competency') as string[];
      expect(options).toEqual(['Excels', 'Exceeds', 'Meets', 'Developing']);
    });

    it('should return 1-10 for policy scale', () => {
      const options = service.getRatingOptions('policy_1_10') as number[];
      expect(options.length).toBe(10);
      expect(options[0]).toBe(1);
      expect(options[9]).toBe(10);
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// Property 9: Dynamic Form Rendering Completeness
// Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Minimal seeded PRNG (mulberry32) so tests are deterministic per seed
 * while still covering a wide range of inputs across 100 iterations.
 */
function mulberry32(seed: number): () => number {
  return function () {
    seed |= 0;
    seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

type RandFn = () => number;

function randInt(rand: RandFn, min: number, max: number): number {
  return Math.floor(rand() * (max - min + 1)) + min;
}

function randItem<T>(rand: RandFn, arr: T[]): T {
  return arr[Math.floor(rand() * arr.length)];
}

type DynamicSectionType = 'key_responsibilities' | 'idp' | 'policy_adherence' | 'goals';

const DYNAMIC_SECTION_TYPES: DynamicSectionType[] = [
  'key_responsibilities',
  'idp',
  'policy_adherence',
  'goals',
];

function ratingScaleFor(sectionType: DynamicSectionType): 'competency' | 'policy_1_10' {
  return sectionType === 'policy_adherence' ? 'policy_1_10' : 'competency';
}

/**
 * Generate a random valid AppraisalTemplateSchema with:
 * - 1 to 4 dynamic section types (no duplicates)
 * - 1 to 5 items per section
 */
function generateRandomSchema(rand: RandFn): AppraisalTemplateSchema {
  // Shuffle section types and pick a random subset (1..4)
  const shuffled = [...DYNAMIC_SECTION_TYPES].sort(() => rand() - 0.5);
  const count = randInt(rand, 1, DYNAMIC_SECTION_TYPES.length);
  const chosenTypes = shuffled.slice(0, count);

  const sections = chosenTypes.map((sectionType, sIdx) => {
    const itemCount = randInt(rand, 1, 5);
    const items = Array.from({ length: itemCount }, (_, i) => ({
      id: `${sectionType}_${sIdx}_${i}`,
      label: `Item ${i + 1} of ${sectionType}`,
      ratingScale: ratingScaleFor(sectionType) as 'competency' | 'policy_1_10',
    }));
    return { sectionType, title: `Section ${sectionType}`, items };
  });

  return { version: '3.0', sections };
}

describe('Property 9: Dynamic Form Rendering Completeness', () => {
  /**
   * **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10**
   *
   * For any valid AppraisalTemplate JSON with a defined set of sections and items,
   * the rendered Appraisal_Form SHALL contain all sections defined in the template
   * with all required fields present for each item.
   */
  let service: FormRendererService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection()],
    });
    service = TestBed.inject(FormRendererService);
  });

  it('runs 100 iterations: every template section appears in rendered output', () => {
    for (let seed = 0; seed < 100; seed++) {
      const rand = mulberry32(seed * 1337 + 42);
      const schema = generateRandomSchema(rand);
      const rendered = service.renderSections(schema);

      const renderedTypes = new Set(rendered.map((s) => s.sectionType));

      for (const section of schema.sections) {
        expect(renderedTypes.has(section.sectionType)).toBe(true);
      }
    }
  });

  it('runs 100 iterations: no extra sections appear beyond the template', () => {
    for (let seed = 0; seed < 100; seed++) {
      const rand = mulberry32(seed * 2053 + 7);
      const schema = generateRandomSchema(rand);
      const rendered = service.renderSections(schema);

      // rating_key is excluded by the renderer — all other rendered sections must
      // correspond to a section that was in the template
      const templateTypes = new Set(schema.sections.map((s) => s.sectionType));
      for (const renderedSection of rendered) {
        expect(templateTypes.has(renderedSection.sectionType)).toBe(true);
      }
    }
  });

  it('runs 100 iterations: item count in rendered section matches template', () => {
    for (let seed = 0; seed < 100; seed++) {
      const rand = mulberry32(seed * 997 + 13);
      const schema = generateRandomSchema(rand);
      const rendered = service.renderSections(schema);

      for (const templateSection of schema.sections) {
        const renderedSection = rendered.find(
          (r) => r.sectionType === templateSection.sectionType
        );
        expect(renderedSection).toBeDefined();
        const expectedItemCount = templateSection.items?.length ?? 0;
        expect(renderedSection!.items.length).toBe(expectedItemCount);
      }
    }
  });

  it('runs 100 iterations: key_responsibilities items have competency dataKeys', () => {
    for (let seed = 0; seed < 100; seed++) {
      const rand = mulberry32(seed * 1231 + 99);
      const schema = generateRandomSchema(rand);

      const krTemplateSection = schema.sections.find(
        (s) => s.sectionType === 'key_responsibilities'
      );
      if (!krTemplateSection) continue; // section not in this iteration's template

      const rendered = service.renderSections(schema);
      const krSection = rendered.find((s) => s.sectionType === 'key_responsibilities')!;

      krSection.items.forEach((item, idx) => {
        // dataKey must reference the keyResponsibilities array slot
        expect(item.dataKey).toBe(`keyResponsibilities[${idx}]`);
        // ratingScale must be competency for self/manager rating fields
        expect(item.ratingScale).toBe('competency');
      });
    }
  });

  it('runs 100 iterations: idp items have competency dataKeys', () => {
    for (let seed = 0; seed < 100; seed++) {
      const rand = mulberry32(seed * 1543 + 17);
      const schema = generateRandomSchema(rand);

      const idpTemplateSection = schema.sections.find((s) => s.sectionType === 'idp');
      if (!idpTemplateSection) continue;

      const rendered = service.renderSections(schema);
      const idpSection = rendered.find((s) => s.sectionType === 'idp')!;

      idpSection.items.forEach((item, idx) => {
        expect(item.dataKey).toBe(`idp[${idx}]`);
        expect(item.ratingScale).toBe('competency');
      });
    }
  });

  it('runs 100 iterations: goals items have competency dataKeys', () => {
    for (let seed = 0; seed < 100; seed++) {
      const rand = mulberry32(seed * 1789 + 31);
      const schema = generateRandomSchema(rand);

      const goalsTemplateSection = schema.sections.find((s) => s.sectionType === 'goals');
      if (!goalsTemplateSection) continue;

      const rendered = service.renderSections(schema);
      const goalsSection = rendered.find((s) => s.sectionType === 'goals')!;

      goalsSection.items.forEach((item, idx) => {
        expect(item.dataKey).toBe(`goals[${idx}]`);
        expect(item.ratingScale).toBe('competency');
      });
    }
  });

  it('runs 100 iterations: policy_adherence items use policy_1_10 rating scale', () => {
    for (let seed = 0; seed < 100; seed++) {
      const rand = mulberry32(seed * 2311 + 53);
      const schema = generateRandomSchema(rand);

      const policyTemplateSection = schema.sections.find(
        (s) => s.sectionType === 'policy_adherence'
      );
      if (!policyTemplateSection) continue;

      const rendered = service.renderSections(schema);
      const policySection = rendered.find((s) => s.sectionType === 'policy_adherence')!;

      for (const item of policySection.items) {
        // manager-rating for policy adherence must use 1-10 scale (Req 4.7)
        expect(item.ratingScale).toBe('policy_1_10');
      }
    }
  });

  it('runs 100 iterations: rendered items preserve id and label from template', () => {
    for (let seed = 0; seed < 100; seed++) {
      const rand = mulberry32(seed * 3001 + 77);
      const schema = generateRandomSchema(rand);
      const rendered = service.renderSections(schema);

      for (const templateSection of schema.sections) {
        const renderedSection = rendered.find(
          (r) => r.sectionType === templateSection.sectionType
        );
        expect(renderedSection).toBeDefined();

        (templateSection.items ?? []).forEach((templateItem, idx) => {
          const renderedItem = renderedSection!.items[idx];
          expect(renderedItem.id).toBe(templateItem.id);
          expect(renderedItem.label).toBe(templateItem.label);
        });
      }
    }
  });

  it('runs 100 iterations: parseTemplateSchema round-trips any generated schema', () => {
    for (let seed = 0; seed < 100; seed++) {
      const rand = mulberry32(seed * 4001 + 11);
      const schema = generateRandomSchema(rand);
      const json = JSON.stringify(schema);

      const parsed = service.parseTemplateSchema(json);

      expect(parsed.version).toBe(schema.version);
      expect(parsed.sections.length).toBe(schema.sections.length);

      parsed.sections.forEach((parsedSection, idx) => {
        expect(parsedSection.sectionType).toBe(schema.sections[idx].sectionType);
        expect(parsedSection.items?.length).toBe(schema.sections[idx].items?.length ?? 0);
      });
    }
  });
});
