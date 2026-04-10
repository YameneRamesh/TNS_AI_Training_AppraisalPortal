/**
 * Type definitions for the Appraisal Template JSON schema
 * and the rendered form structure used by the form renderer.
 */

export type SectionType =
  | 'header'
  | 'rating_key'
  | 'overall_evaluation'
  | 'key_responsibilities'
  | 'idp'
  | 'policy_adherence'
  | 'goals'
  | 'next_year_goals'
  | 'signature';

export type RatingScale = 'competency' | 'policy_1_10';

/** A single item within a template section (e.g. one Key Responsibility row) */
export interface TemplateItem {
  id: string;
  label: string;
  ratingScale: RatingScale;
}

/** A section definition from the template schema_json */
export interface TemplateSection {
  sectionType: SectionType;
  title: string;
  items?: TemplateItem[];
}

/** Root structure of the schema_json stored in appraisal_templates */
export interface AppraisalTemplateSchema {
  version: string;
  sections: TemplateSection[];
}

/** Resolved, ready-to-render representation of a form section */
export interface RenderedSection {
  sectionType: SectionType;
  title: string;
  items: RenderedItem[];
}

/** A single rendered row with its data-binding key and display metadata */
export interface RenderedItem {
  id: string;
  label: string;
  ratingScale: RatingScale;
  /** dot-path key into FormData for self-comment, e.g. "keyResponsibilities[0].selfComment" */
  dataKey: string;
}

/** Describes which fields are editable for a given user role and form status */
export interface FieldEditability {
  selfCommentEditable: boolean;
  selfRatingEditable: boolean;
  managerCommentEditable: boolean;
  managerRatingEditable: boolean;
}
