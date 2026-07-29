import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { coerceProposal, maskApiKey, normalizeProposals, parseToolArguments } from './proposals'

describe('proposals', () => {
  it('masks api keys', () => {
    assert.equal(maskApiKey('gsk_abcdefghijklmnop'), 'gsk_ab…mnop')
  })

  it('parses tool arguments', () => {
    assert.deepEqual(parseToolArguments('{"query":"Sodium"}'), { query: 'Sodium' })
    assert.deepEqual(parseToolArguments('not-json'), {})
  })

  it('normalizes install proposals', () => {
    const list = normalizeProposals([
      {
        project_id: 'AANobbMI',
        title: 'Sodium',
        source: 'modrinth',
        project_type: 'mod',
        downloads: 100
      },
      {
        projectId: 'AANobbMI',
        title: 'Sodium',
        source: 'modrinth',
        type: 'mod'
      },
      { title: 'broken' }
    ])
    assert.equal(list.length, 1)
    assert.equal(list[0]?.projectId, 'AANobbMI')
    assert.equal(list[0]?.source, 'modrinth')
  })

  it('coerces curseforge shader proposals', () => {
    const p = coerceProposal({
      projectId: '123',
      title: 'Complementary',
      source: 'curseforge',
      type: 'shader'
    })
    assert.ok(p)
    assert.equal(p?.type, 'shader')
    assert.equal(p?.source, 'curseforge')
  })
})
