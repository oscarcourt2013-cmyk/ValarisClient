import { motion } from 'framer-motion'
import { PrimeLogo, ProgressBar } from '@renderer/design-system/components'
import { useI18n } from '@renderer/context/I18nProvider'
import { BOOT_STEPS } from '@shared/types'
import './SplashScreen.css'

interface SplashScreenProps {
  progress: number
  stepIndex: number
  version: string
}

export function SplashScreen({ progress, stepIndex, version }: SplashScreenProps) {
  const { t } = useI18n()
  const step = BOOT_STEPS[stepIndex]
  const particles = Array.from({ length: 24 }, (_, i) => ({
    id: i,
    x: Math.random() * 100,
    y: Math.random() * 100,
    delay: Math.random() * 2,
    size: 1 + Math.random() * 2
  }))

  return (
    <motion.div
      className="splash"
      exit={{ opacity: 0, scale: 1.02 }}
      transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
    >
      <div className="splash__bg" />
      <div className="splash__particles">
        {particles.map((p) => (
          <motion.div
            key={p.id}
            className="splash__particle"
            style={{ left: `${p.x}%`, top: `${p.y}%`, width: p.size, height: p.size }}
            animate={{ opacity: [0.2, 0.7, 0.2], y: [0, -12, 0] }}
            transition={{ duration: 3 + p.delay, repeat: Infinity, delay: p.delay }}
          />
        ))}
      </div>

      <div className="splash__content">
        <motion.div
          className="splash__logo-wrap"
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
        >
          <div className="splash__glow" />
          <PrimeLogo size={120} />
        </motion.div>

        <motion.div
          className="splash__status"
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.5 }}
        >
          <div className={`splash__step${stepIndex >= 0 ? ' splash__step--active' : ''}`}>
            {step ? t(`boot.${step.id}`) : ''}
          </div>
          <ProgressBar value={progress} large />
        </motion.div>
      </div>

      <span className="splash__version">v{version}</span>
    </motion.div>
  )
}
